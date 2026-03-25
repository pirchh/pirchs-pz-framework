require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISTextEntryBox"

print("[PZLIFE][LUA][debug] PirchsPZDBI_Debug.lua loaded")

local FONT_SMALL = UIFont.Small
local FONT_MEDIUM = UIFont.Medium
local FONT_LARGE = UIFont.Large
local FONT_HGT_SMALL = getTextManager():getFontHeight(FONT_SMALL)
local HOTKEY = (Keyboard and Keyboard.KEY_F6) or 64
local MENU = nil

local TAB_ORDER = { "Identity", "Nodes", "Permissions", "Roles", "Utility" }

local TAB_ACTIONS = {
    Identity = {
        { id = "READY", label = "Identity Ready" },
        { id = "SNAPSHOT", label = "Local Snapshot" },
        { id = "SELF_TEST_NOW", label = "Self Test Now" },
        { id = "SELF_TEST_STATUS", label = "Self Test Status" },
        { id = "RESET_LIFECYCLE", label = "Reset Lifecycle" },
        { id = "RUN_SMOKE", label = "Run Smoke Suite" },
        { id = "LIST_OWNED_NODES", label = "List Owned Nodes" },
    },
    Nodes = {
        { id = "CLAIM_NODE", label = "Claim Node" },
        { id = "RELEASE_NODE", label = "Release Node" },
        { id = "GET_NODE_OWNER", label = "Get Node Owner" },
        { id = "LIST_OWNED_NODES", label = "List Owned Nodes" },
    },
    Permissions = {
        { id = "GRANT_PERMISSION", label = "Grant Permission" },
        { id = "REVOKE_PERMISSION", label = "Revoke Permission" },
        { id = "HAS_PERMISSION", label = "Has Permission" },
        { id = "EXPLAIN_PERMISSION", label = "Explain Permission" },
        { id = "LIST_PERMISSIONS", label = "List Permissions" },
    },
    Roles = {
        { id = "ASSIGN_ROLE", label = "Assign Role" },
        { id = "REVOKE_ROLE", label = "Revoke Role" },
        { id = "HAS_ROLE", label = "Has Role" },
        { id = "LIST_ROLES", label = "List Roles" },
    },
    Utility = {
        { id = "REFRESH_STATUS", label = "Refresh Bridge Status" },
        { id = "LOG_DEFAULTS", label = "Log Current Inputs" },
        { id = "CLEAR_LOG", label = "Clear Log" },
        { id = "CLOSE", label = "Close Console" },
    }
}

PirchsPZDBIDebugMenu = ISPanel:derive("PirchsPZDBIDebugMenu")

local function clip(text, maxLen)
    text = tostring(text or "")
    if #text <= maxLen then return text end
    if maxLen <= 3 then return text:sub(1, maxLen) end
    return text:sub(1, maxLen - 3) .. "..."
end

local function safeText(widget, fallback)
    if widget and widget.getInternalText then
        local value = widget:getInternalText()
        if value ~= nil and tostring(value) ~= "" then
            return tostring(value)
        end
    end
    return fallback
end

local function hasDebugFacade()
    return pz and pz.bridge and pz.bridge.debug and pz.bridge.debug.isAvailable and pz.bridge.debug.isAvailable()
end

function PirchsPZDBIDebugMenu:new(x, y, w, h)
    local o = ISPanel:new(x, y, w, h)
    setmetatable(o, self)
    self.__index = self
    o.backgroundColor = { r = 0.02, g = 0.04, b = 0.06, a = 0.97 }
    o.borderColor = { r = 0.16, g = 0.58, b = 0.88, a = 1.0 }
    o.moveWithMouse = true
    o.lines = {}
    o.lastResult = "idle"
    o.lastResultWrapped = { "idle" }
    o.statusText = "UNKNOWN"
    o.statusColor = { r = 0.95, g = 0.75, b = 0.22, a = 1.0 }
    o.activeTab = "Identity"
    o.tabButtons = {}
    o.actionButtons = {}
    return o
end

function PirchsPZDBIDebugMenu:initialise()
    ISPanel.initialise(self)
    self:create()
end

function PirchsPZDBIDebugMenu:drawSection(x, y, w, h, title)
    self:drawRect(x, y, w, h, 0.26, 0.01, 0.05, 0.08)
    self:drawRect(x, y, w, 26, 0.55, 0.03, 0.09, 0.14)
    self:drawRectBorder(x, y, w, h, 0.85, 0.16, 0.55, 0.86)
    self:drawText(title, x + 10, y + 5, 0.95, 0.97, 1.0, 1.0, FONT_MEDIUM)
end

function PirchsPZDBIDebugMenu:prerender()
    ISPanel.prerender(self)
    self:updateBridgeStatus()

    self:drawText("PZLife Debug Console", 18, 14, 1, 1, 1, 1, FONT_LARGE)
    self:drawText("F6 to toggle", 270, 18, 0.70, 0.82, 0.92, 1, FONT_MEDIUM)

    self:drawRect(self.width - 126, 12, 108, 30, 0.15, 0.10, 0.02, 0.02)
    self:drawRectBorder(self.width - 126, 12, 108, 30, 1.0, self.statusColor.r, self.statusColor.g, self.statusColor.b)
    self:drawTextCentre(self.statusText, self.width - 72, 17, self.statusColor.r, self.statusColor.g, self.statusColor.b, 1, FONT_MEDIUM)

    self:drawSection(16, 56, 350, 188, "Context")
    self:drawSection(382, 56, self.width - 398, 188, "Status")
    self:drawSection(16, 260, 156, 250, "Sections")
    self:drawSection(188, 260, self.width - 204, 250, "Actions")
    self:drawSection(16, 526, self.width - 32, self.height - 542, "Recent Log")

    self:drawText("Node Key", 30, 88, 0.95, 0.95, 0.95, 1, FONT_SMALL)
    self:drawText("Node Type", 30, 144, 0.95, 0.95, 0.95, 1, FONT_SMALL)
    self:drawText("Permission", 194, 88, 0.95, 0.95, 0.95, 1, FONT_SMALL)
    self:drawText("Role", 194, 144, 0.95, 0.95, 0.95, 1, FONT_SMALL)

    self:drawText("Bridge Status", 398, 88, 0.74, 0.86, 0.96, 1, FONT_SMALL)
    self:drawText(self.statusText == "READY" and "debug facade attached" or "debug facade unavailable", 398, 108, 1, 1, 1, 1, FONT_SMALL)
    self:drawText("Current Section", 398, 140, 0.74, 0.86, 0.96, 1, FONT_SMALL)
    self:drawText(self.activeTab, 398, 160, 1, 1, 1, 1, FONT_MEDIUM)
    self:drawText("Last Result", 398, 192, 0.74, 0.86, 0.96, 1, FONT_SMALL)

    local ry = 212
    for i = 1, #self.lastResultWrapped do
        local line = self.lastResultWrapped[i]
        if line and line ~= "" then
            self:drawText(line, 398, ry, 0.95, 0.95, 0.95, 1, FONT_SMALL)
            ry = ry + FONT_HGT_SMALL + 3
        end
    end

    local logY = 560
    for i = 1, #self.lines do
        self:drawText(self.lines[i], 28, logY, 0.92, 0.92, 0.92, 1, FONT_SMALL)
        logY = logY + FONT_HGT_SMALL + 5
    end
end

function PirchsPZDBIDebugMenu:updateBridgeStatus()
    if hasDebugFacade() then
        self.statusText = "READY"
        self.statusColor = { r = 0.35, g = 0.95, b = 0.55, a = 1.0 }
    else
        self.statusText = "ERROR"
        self.statusColor = { r = 1.0, g = 0.35, b = 0.35, a = 1.0 }
    end
end

function PirchsPZDBIDebugMenu:updateWrappedResult(text)
    local s = tostring(text or "idle")
    self.lastResultWrapped = {
        clip(s, 66),
        (#s > 66) and clip(s:sub(67), 66) or "",
        (#s > 132) and clip(s:sub(133), 66) or ""
    }
    while #self.lastResultWrapped > 0 and self.lastResultWrapped[#self.lastResultWrapped] == "" do
        table.remove(self.lastResultWrapped)
    end
end

function PirchsPZDBIDebugMenu:createTextBox(x, y, w, default)
    local box = ISTextEntryBox:new(default, x, y, w, 30)
    box:initialise()
    box:instantiate()
    self:addChild(box)
    return box
end

function PirchsPZDBIDebugMenu:createTabButton(x, y, w, h, label)
    local btn = ISButton:new(x, y, w, h, label, self, PirchsPZDBIDebugMenu.onTabClicked)
    btn.internal = label
    btn:initialise()
    btn:instantiate()
    btn.borderColor = { r = 0.18, g = 0.62, b = 0.92, a = 0.95 }
    btn.backgroundColor = { r = 0.04, g = 0.10, b = 0.16, a = 0.95 }
    btn.backgroundColorMouseOver = { r = 0.07, g = 0.16, b = 0.24, a = 0.98 }
    self:addChild(btn)
    self.tabButtons[label] = btn
end

function PirchsPZDBIDebugMenu:addActionButton(x, y, w, h, title, internal)
    local btn = ISButton:new(x, y, w, h, title, self, PirchsPZDBIDebugMenu.onOptionMouseDown)
    btn.internal = internal
    btn:initialise()
    btn:instantiate()
    btn.borderColor = { r = 0.18, g = 0.62, b = 0.92, a = 0.95 }
    btn.backgroundColor = { r = 0.04, g = 0.10, b = 0.16, a = 0.95 }
    btn.backgroundColorMouseOver = { r = 0.07, g = 0.16, b = 0.24, a = 0.98 }
    self:addChild(btn)
    self.actionButtons[internal] = btn
    return btn
end

function PirchsPZDBIDebugMenu:layoutActionButtonsForTab(tab)
    local defs = TAB_ACTIONS[tab] or {}
    local actionX = 208
    local actionY = 302
    local actionW = 270
    local actionH = 36
    local gapX = 18
    local gapY = 12
    local maxRows = 4
    local col = 0
    local row = 0

    for _, btn in pairs(self.actionButtons) do
        btn:setVisible(false)
    end

    for _, def in ipairs(defs) do
        local btn = self.actionButtons[def.id]
        if btn then
            btn:setX(actionX + col * (actionW + gapX))
            btn:setY(actionY + row * (actionH + gapY))
            btn:setWidth(actionW)
            btn:setHeight(actionH)
            btn:setVisible(true)

            row = row + 1
            if row >= maxRows then
                row = 0
                col = col + 1
            end
        end
    end
end

function PirchsPZDBIDebugMenu:create()
    self.nodeKey = self:createTextBox(30, 106, 148, "test:node_debug_1")
    self.permission = self:createTextBox(194, 106, 148, "debug.use")
    self.nodeType = self:createTextBox(30, 162, 148, "node")
    self.role = self:createTextBox(194, 162, 148, "owner")

    local tabY = 296
    for i = 1, #TAB_ORDER do
        self:createTabButton(32, tabY, 124, 34, TAB_ORDER[i])
        tabY = tabY + 42
    end

    local created = {}
    for _, tabName in ipairs(TAB_ORDER) do
        local defs = TAB_ACTIONS[tabName]
        for _, def in ipairs(defs) do
            if not created[def.id] then
                self:addActionButton(0, 0, 270, 36, def.label, def.id)
                created[def.id] = true
            end
        end
    end

    self:setActiveTab(self.activeTab)
end

function PirchsPZDBIDebugMenu:pushLine(text)
    table.insert(self.lines, 1, clip(text, 132))
    while #self.lines > 7 do
        table.remove(self.lines)
    end
end

function PirchsPZDBIDebugMenu:getBridge()
    if not hasDebugFacade() then
        return nil, "debug facade unavailable"
    end
    return pz.bridge.debug, nil
end

function PirchsPZDBIDebugMenu:finishCall(label, ok, value)
    self.lastResult = label .. " => ok=" .. tostring(ok) .. " value=" .. tostring(value)
    self:updateWrappedResult(self.lastResult)
    self:pushLine((ok and "[OK] " or "[ERR] ") .. label .. " => " .. tostring(value))
    print("[PZLIFE][LUA][debugmenu] " .. self.lastResult)
end

function PirchsPZDBIDebugMenu:invoke(label, fn)
    local bridge, err = self:getBridge()
    if not bridge then
        self:finishCall(label, false, err)
        return
    end
    local ok, value = pcall(fn, bridge)
    self:finishCall(label, ok, value)
end

function PirchsPZDBIDebugMenu:setActiveTab(tab)
    self.activeTab = tab
    for name, btn in pairs(self.tabButtons) do
        if name == tab then
            btn.backgroundColor = { r = 0.12, g = 0.24, b = 0.34, a = 1.0 }
        else
            btn.backgroundColor = { r = 0.04, g = 0.10, b = 0.16, a = 0.95 }
        end
    end

    self:layoutActionButtonsForTab(tab)
    self:updateWrappedResult(self.lastResult)
end

function PirchsPZDBIDebugMenu:onTabClicked(button)
    self:setActiveTab(button.internal)
end

function PirchsPZDBIDebugMenu:onOptionMouseDown(button)
    local nodeKey = safeText(self.nodeKey, "test:node_debug_1")
    local nodeType = safeText(self.nodeType, "node")
    local permission = safeText(self.permission, "debug.use")
    local role = safeText(self.role, "owner")

    if button.internal == "READY" then
        self:invoke("isLocalIdentityReady", function(bridge) return bridge.isLocalIdentityReady() end)
    elseif button.internal == "SNAPSHOT" then
        self:invoke("localSnapshot", function(bridge) return bridge.localSnapshot() end)
    elseif button.internal == "SELF_TEST_NOW" then
        self:invoke("selfTestNow", function(bridge) return bridge.selfTestNow() end)
    elseif button.internal == "SELF_TEST_STATUS" then
        self:invoke("selfTestStatus", function(bridge) return bridge.selfTestStatus() end)
    elseif button.internal == "RESET_LIFECYCLE" then
        self:invoke("resetLifecycle", function(bridge) return bridge.resetLifecycle() end)
    elseif button.internal == "RUN_SMOKE" then
        self:invoke("runSmokeSuite", function(bridge) return bridge.runSmokeSuite(nodeKey, nodeType) end)
    elseif button.internal == "CLAIM_NODE" then
        self:invoke("claimNode", function(bridge) return bridge.claimNode(nodeKey, nodeType) end)
    elseif button.internal == "RELEASE_NODE" then
        self:invoke("releaseNode", function(bridge) return bridge.releaseNode(nodeKey) end)
    elseif button.internal == "GET_NODE_OWNER" then
        self:invoke("getNodeOwner", function(bridge) return bridge.getNodeOwner(nodeKey) end)
    elseif button.internal == "LIST_OWNED_NODES" then
        self:invoke("listOwnedNodes", function(bridge) return bridge.listOwnedNodes() end)
    elseif button.internal == "GRANT_PERMISSION" then
        self:invoke("grantPermissionToSelf", function(bridge) return bridge.grantPermissionToSelf(permission) end)
    elseif button.internal == "REVOKE_PERMISSION" then
        self:invoke("revokePermissionFromSelf", function(bridge) return bridge.revokePermissionFromSelf(permission) end)
    elseif button.internal == "HAS_PERMISSION" then
        self:invoke("hasPermission", function(bridge) return bridge.hasPermission(permission) end)
    elseif button.internal == "EXPLAIN_PERMISSION" then
        self:invoke("explainPermission", function(bridge) return bridge.explainPermission(permission) end)
    elseif button.internal == "LIST_PERMISSIONS" then
        self:invoke("listPermissions", function(bridge) return bridge.listPermissions() end)
    elseif button.internal == "ASSIGN_ROLE" then
        self:invoke("assignRoleToSelf", function(bridge) return bridge.assignRoleToSelf(role) end)
    elseif button.internal == "REVOKE_ROLE" then
        self:invoke("revokeRoleFromSelf", function(bridge) return bridge.revokeRoleFromSelf(role) end)
    elseif button.internal == "HAS_ROLE" then
        self:invoke("hasRole", function(bridge) return bridge.hasRole(role) end)
    elseif button.internal == "LIST_ROLES" then
        self:invoke("listRoles", function(bridge) return bridge.listRoles() end)
    elseif button.internal == "REFRESH_STATUS" then
        self:updateBridgeStatus()
        self:pushLine("bridge status refreshed")
    elseif button.internal == "LOG_DEFAULTS" then
        self:pushLine("nodeKey=" .. nodeKey .. " | nodeType=" .. nodeType .. " | permission=" .. permission .. " | role=" .. role)
    elseif button.internal == "CLEAR_LOG" then
        self.lines = {}
        self.lastResult = "idle"
        self:updateWrappedResult(self.lastResult)
    elseif button.internal == "CLOSE" then
        self:setVisible(false)
        self:removeFromUIManager()
        MENU = nil
    end
end

local function createMenu()
    local width = 1040
    local height = 760
    local x = math.max(20, math.floor((getCore():getScreenWidth() - width) / 2))
    local y = math.max(20, math.floor((getCore():getScreenHeight() - height) / 2))
    local ui = PirchsPZDBIDebugMenu:new(x, y, width, height)
    ui:initialise()
    ui:addToUIManager()
    ui:setVisible(true)
    ui:updateWrappedResult("idle")
    ui:pushLine("debug menu opened")
    ui:pushLine("lua exposer/global facade retry enabled")
    ui:pushLine("nodeKey=test:node_debug_1 | nodeType=node | permission=debug.use | role=owner")
    return ui
end

local function logLine(text)
    print("[PZLIFE][LUA][debugmenu] " .. tostring(text))
    if MENU and MENU.pushLine then
        MENU:pushLine(tostring(text))
    end
end

local function toggleMenu()
    if MENU and MENU:getIsVisible() then
        MENU:setVisible(false)
        MENU:removeFromUIManager()
        MENU = nil
        print("[PZLIFE][LUA][debugmenu] debug menu closed")
        return
    end

    MENU = createMenu()
end

local function onKeyPressed(key)
    if key ~= HOTKEY then return end
    toggleMenu()
end

local function onGameStart()
    logLine("press F6 to open the PZLife debug console")
end

Events.OnKeyPressed.Add(onKeyPressed)
Events.OnGameStart.Add(onGameStart)
