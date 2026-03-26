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

local METHOD = {
    READY = "pz.bridge.debug.isLocalIdentityReady",
    SNAPSHOT = "pz.bridge.debug.localSnapshot",
    SELF_TEST_NOW = "pz.bridge.debug.selfTestNow",
    SELF_TEST_STATUS = "pz.bridge.debug.selfTestStatus",
    RESET_LIFECYCLE = "pz.bridge.debug.resetLifecycle",
    RUN_SMOKE = "pz.bridge.debug.runSmokeSuite",
    CLAIM_NODE = "pz.bridge.debug.claimNode",
    RELEASE_NODE = "pz.bridge.debug.releaseNode",
    GET_NODE_OWNER = "pz.bridge.debug.getNodeOwner",
    LIST_OWNED_NODES = "pz.bridge.debug.listOwnedNodes",
    GRANT_PERMISSION = "pz.bridge.debug.grantPermissionToSelf",
    REVOKE_PERMISSION = "pz.bridge.debug.revokePermissionFromSelf",
    HAS_PERMISSION = "pz.bridge.debug.hasPermission",
    EXPLAIN_PERMISSION = "pz.bridge.debug.explainPermission",
    LIST_PERMISSIONS = "pz.bridge.debug.listPermissions",
    ASSIGN_ROLE = "pz.bridge.debug.assignRoleToSelf",
    REVOKE_ROLE = "pz.bridge.debug.revokeRoleFromSelf",
    HAS_ROLE = "pz.bridge.debug.hasRole",
    LIST_ROLES = "pz.bridge.debug.listRoles",
}

PirchsPZDBIDebugMenu = ISPanel:derive("PirchsPZDBIDebugMenu")

local function log(v)
    print("[PZLIFE][LUA][debug] " .. tostring(v))
end

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

local function hasGlobalBridge()
    return type(pzlife) == "function"
        and type(pzlife_has) == "function"
        and type(pzlife_invoke0) == "function"
        and type(pzlife_invoke1) == "function"
        and type(pzlife_invoke2) == "function"
        and type(pzlife_invoke3) == "function"
end

local function getBridgeStatus()
    local status = {
        present = hasGlobalBridge(),
        hasPzlife = type(pzlife) == "function",
        hasHas = type(pzlife_has) == "function",
        hasInvoke0 = type(pzlife_invoke0) == "function",
        hasInvoke1 = type(pzlife_invoke1) == "function",
        hasInvoke2 = type(pzlife_invoke2) == "function",
        hasInvoke3 = type(pzlife_invoke3) == "function",
    }

    if type(pzlife_bridge_status) == "function" then
        local ok, value = pcall(function()
            return pzlife_bridge_status()
        end)
        status.bridgeStatusCallOk = ok
        status.bridgeStatusCallValue = value
    end

    return status
end

local function invokeBridgeMethod(methodName, ...)
    if not hasGlobalBridge() then
        return false, nil, "PZLife global bridge unavailable"
    end

    local argc = select("#", ...)
    local ok, result

    if argc == 0 then
        ok, result = pcall(function()
            return pzlife_invoke0(methodName)
        end)
    elseif argc == 1 then
        local a1 = ...
        ok, result = pcall(function()
            return pzlife_invoke1(methodName, a1)
        end)
    elseif argc == 2 then
        local a1, a2 = ...
        ok, result = pcall(function()
            return pzlife_invoke2(methodName, a1, a2)
        end)
    elseif argc == 3 then
        local a1, a2, a3 = ...
        ok, result = pcall(function()
            return pzlife_invoke3(methodName, a1, a2, a3)
        end)
    else
        return false, nil, "Too many arguments for debug bridge: " .. tostring(argc)
    end

    if not ok then
        return false, nil, "global invoke failed: " .. tostring(result)
    end

    return true, result, nil
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
    o.statusColor = { r = 0.95, g = 0.85, b = 0.25, a = 1.0 }
    o.activeTab = TAB_ORDER[1]
    o.nodeKeyEntry = nil
    o.nodeTypeEntry = nil
    o.permissionEntry = nil
    o.roleEntry = nil
    o.resultPanelX = 0
    o.resultPanelY = 0
    o.resultPanelW = 0
    o.resultPanelH = 0
    o.logPanelX = 0
    o.logPanelY = 0
    o.logPanelW = 0
    o.logPanelH = 0
    o.tabButtons = {}
    o.actionButtons = {}
    o.bridgeStatusSnapshot = {}
    return o
end

function PirchsPZDBIDebugMenu:pushLine(text)
    self.lines[#self.lines + 1] = tostring(text)
    while #self.lines > 200 do
        table.remove(self.lines, 1)
    end
end

function PirchsPZDBIDebugMenu:updateWrappedResult(text)
    local source = tostring(text or "")
    local wrapWidth = math.max(220, self.width - 90)
    local manager = getTextManager()
    local words = {}
    for token in source:gmatch("%S+") do
        words[#words + 1] = token
    end

    local lines = {}
    local current = ""

    local function commit()
        if current ~= "" then
            lines[#lines + 1] = current
            current = ""
        end
    end

    if #words == 0 then
        lines[1] = ""
    else
        for i = 1, #words do
            local nextValue = words[i]
            local candidate = current == "" and nextValue or (current .. " " .. nextValue)
            local width = manager:MeasureStringX(FONT_SMALL, candidate)
            if width <= wrapWidth or current == "" then
                current = candidate
            else
                commit()
                current = nextValue
            end
        end
        commit()
    end

    self.lastResultWrapped = lines
end

function PirchsPZDBIDebugMenu:updateBridgeStatus()
    self.bridgeStatusSnapshot = getBridgeStatus()

    if self.bridgeStatusSnapshot.present then
        self.statusText = "READY"
        self.statusColor = { r = 0.35, g = 0.95, b = 0.55, a = 1.0 }
    else
        self.statusText = "ERROR"
        self.statusColor = { r = 1.0, g = 0.35, b = 0.35, a = 1.0 }
    end
end

function PirchsPZDBIDebugMenu:finishCall(label, ok, value)
    if ok then
        self.lastResult = tostring(value)
        self:updateWrappedResult(self.lastResult)
        self:pushLine(tostring(label) .. " => ok=true value=" .. clip(self.lastResult, 220))
        log(tostring(label) .. " => ok=true value=" .. clip(self.lastResult, 220))
    else
        self.lastResult = tostring(value)
        self:updateWrappedResult(self.lastResult)
        self:pushLine(tostring(label) .. " => ok=false value=" .. clip(self.lastResult, 220))
        log(tostring(label) .. " => ok=false value=" .. clip(self.lastResult, 220))
    end
    self:updateBridgeStatus()
end

function PirchsPZDBIDebugMenu:invokeById(actionId, ...)
    local methodName = METHOD[actionId]
    if methodName == nil then
        self:finishCall(actionId, false, "No method mapping configured")
        return
    end

    if not hasGlobalBridge() then
        self:finishCall(methodName, false, "PZLife global bridge unavailable")
        return
    end

    local okHas, supported = pcall(function()
        return pzlife_has(methodName)
    end)

    if not okHas then
        self:finishCall(methodName, false, "pzlife_has failed: " .. tostring(supported))
        return
    end

    if not supported then
        self:finishCall(methodName, false, "Method not exposed: " .. tostring(methodName))
        return
    end

    local ok, value, err = invokeBridgeMethod(methodName, ...)
    if ok then
        self:finishCall(methodName, true, value)
    else
        self:finishCall(methodName, false, err or value)
    end
end

function PirchsPZDBIDebugMenu:createChildren()
    local pad = 16
    local top = 44
    local tabW = 128
    local tabH = 28
    local x = pad

    for i = 1, #TAB_ORDER do
        local name = TAB_ORDER[i]
        local btn = ISButton:new(x, top, tabW, tabH, name, self, function(target, button)
            target.activeTab = button.internal
        end)
        btn.internal = name
        btn:initialise()
        btn:instantiate()
        self:addChild(btn)
        self.tabButtons[#self.tabButtons + 1] = btn
        x = x + tabW + 8
    end

    local entryY = top + tabH + 14
    local labelW = 92
    local entryW = 240
    local rowGap = 10
    local entryH = 24

    local function addEntry(label, value, y)
        local lbl = ISLabel:new(pad, y + 4, entryH, label, 1, 1, 1, 1, FONT_SMALL, true)
        lbl:initialise()
        lbl:instantiate()
        self:addChild(lbl)

        local entry = ISTextEntryBox:new(tostring(value), pad + labelW, y, entryW, entryH)
        entry:initialise()
        entry:instantiate()
        self:addChild(entry)
        return entry
    end

    self.nodeKeyEntry = addEntry("Node Key", "test:node_debug_1", entryY)
    self.nodeTypeEntry = addEntry("Node Type", "node", entryY + (entryH + rowGap))
    self.permissionEntry = addEntry("Permission", "debug.use", entryY + ((entryH + rowGap) * 2))
    self.roleEntry = addEntry("Role", "owner", entryY + ((entryH + rowGap) * 3))

    local actionX = pad + labelW + entryW + 32
    local actionY = entryY
    local actionW = 220
    local actionH = 26
    local actionGap = 8

    for t = 1, #TAB_ORDER do
        local tabName = TAB_ORDER[t]
        local list = TAB_ACTIONS[tabName]
        local bucket = {}
        for i = 1, #list do
            local action = list[i]
            local btn = ISButton:new(actionX, actionY + (i - 1) * (actionH + actionGap), actionW, actionH, action.label, self, self.onActionClicked)
            btn.internal = action.id
            btn.tabName = tabName
            btn:initialise()
            btn:instantiate()
            self:addChild(btn)
            bucket[#bucket + 1] = btn
        end
        self.actionButtons[tabName] = bucket
    end

    self.resultPanelX = pad
    self.resultPanelY = entryY + ((entryH + rowGap) * 4) + 18
    self.resultPanelW = self.width - (pad * 2)
    self.resultPanelH = 132

    self.logPanelX = pad
    self.logPanelY = self.resultPanelY + self.resultPanelH + 16
    self.logPanelW = self.width - (pad * 2)
    self.logPanelH = self.height - self.logPanelY - pad

    self:updateBridgeStatus()
end

function PirchsPZDBIDebugMenu:initialise()
    ISPanel.initialise(self)
    self:createChildren()
end

function PirchsPZDBIDebugMenu:onActionClicked(button)
    local nodeKey = safeText(self.nodeKeyEntry, "test:node_debug_1")
    local nodeType = safeText(self.nodeTypeEntry, "node")
    local permission = safeText(self.permissionEntry, "debug.use")
    local role = safeText(self.roleEntry, "owner")

    if button.internal == "READY" then
        self:invokeById("READY")
    elseif button.internal == "SNAPSHOT" then
        self:invokeById("SNAPSHOT")
    elseif button.internal == "SELF_TEST_NOW" then
        self:invokeById("SELF_TEST_NOW")
    elseif button.internal == "SELF_TEST_STATUS" then
        self:invokeById("SELF_TEST_STATUS")
    elseif button.internal == "RESET_LIFECYCLE" then
        self:invokeById("RESET_LIFECYCLE")
    elseif button.internal == "RUN_SMOKE" then
        self:invokeById("RUN_SMOKE")
    elseif button.internal == "CLAIM_NODE" then
        self:invokeById("CLAIM_NODE", nodeKey, nodeType)
    elseif button.internal == "RELEASE_NODE" then
        self:invokeById("RELEASE_NODE", nodeKey)
    elseif button.internal == "GET_NODE_OWNER" then
        self:invokeById("GET_NODE_OWNER", nodeKey)
    elseif button.internal == "LIST_OWNED_NODES" then
        self:invokeById("LIST_OWNED_NODES")
    elseif button.internal == "GRANT_PERMISSION" then
        self:invokeById("GRANT_PERMISSION", permission)
    elseif button.internal == "REVOKE_PERMISSION" then
        self:invokeById("REVOKE_PERMISSION", permission)
    elseif button.internal == "HAS_PERMISSION" then
        self:invokeById("HAS_PERMISSION", permission)
    elseif button.internal == "EXPLAIN_PERMISSION" then
        self:invokeById("EXPLAIN_PERMISSION", permission)
    elseif button.internal == "LIST_PERMISSIONS" then
        self:invokeById("LIST_PERMISSIONS")
    elseif button.internal == "ASSIGN_ROLE" then
        self:invokeById("ASSIGN_ROLE", role)
    elseif button.internal == "REVOKE_ROLE" then
        self:invokeById("REVOKE_ROLE", role)
    elseif button.internal == "HAS_ROLE" then
        self:invokeById("HAS_ROLE", role)
    elseif button.internal == "LIST_ROLES" then
        self:invokeById("LIST_ROLES")
    elseif button.internal == "REFRESH_STATUS" then
        self:updateBridgeStatus()
        if self.statusText == "READY" then
            self:pushLine("bridge status refreshed: PZLife global bridge ready")
        else
            self:pushLine("bridge status refreshed: PZLife global bridge unavailable")
        end
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

function PirchsPZDBIDebugMenu:prerender()
    ISPanel.prerender(self)

    self:drawRect(0, 0, self.width, self.height, self.backgroundColor.a, self.backgroundColor.r, self.backgroundColor.g, self.backgroundColor.b)
    self:drawRectBorder(0, 0, self.width, self.height, self.borderColor.a, self.borderColor.r, self.borderColor.g, self.borderColor.b)

    self:drawText("PZLife Debug Console", 16, 12, 1, 1, 1, 1, FONT_LARGE)
    self:drawText("Status: " .. self.statusText, self.width - 180, 14, self.statusColor.r, self.statusColor.g, self.statusColor.b, self.statusColor.a, FONT_MEDIUM)

    for i = 1, #self.tabButtons do
        local btn = self.tabButtons[i]
        if btn.internal == self.activeTab then
            btn.backgroundColor = { r = 0.18, g = 0.48, b = 0.72, a = 0.95 }
        else
            btn.backgroundColor = { r = 0.10, g = 0.10, b = 0.12, a = 0.90 }
        end
    end

    for tabName, buttons in pairs(self.actionButtons) do
        local visible = (tabName == self.activeTab)
        for i = 1, #buttons do
            buttons[i]:setVisible(visible)
        end
    end

    local resultTitleY = self.resultPanelY - FONT_HGT_SMALL - 6
    self:drawText("Last Result", self.resultPanelX, resultTitleY, 0.85, 0.95, 1.0, 1.0, FONT_MEDIUM)
    self:drawRect(self.resultPanelX, self.resultPanelY, self.resultPanelW, self.resultPanelH, 0.20, 0.02, 0.02, 0.02)
    self:drawRectBorder(self.resultPanelX, self.resultPanelY, self.resultPanelW, self.resultPanelH, 0.9, 0.18, 0.30, 0.42)

    local resultY = self.resultPanelY + 8
    for i = 1, #self.lastResultWrapped do
        self:drawText(self.lastResultWrapped[i], self.resultPanelX + 8, resultY, 0.92, 0.92, 0.92, 1.0, FONT_SMALL)
        resultY = resultY + FONT_HGT_SMALL + 2
        if resultY > self.resultPanelY + self.resultPanelH - FONT_HGT_SMALL - 4 then
            break
        end
    end

    local logTitleY = self.logPanelY - FONT_HGT_SMALL - 6
    self:drawText("Activity Log", self.logPanelX, logTitleY, 0.85, 0.95, 1.0, 1.0, FONT_MEDIUM)
    self:drawRect(self.logPanelX, self.logPanelY, self.logPanelW, self.logPanelH, 0.20, 0.02, 0.02, 0.02)
    self:drawRectBorder(self.logPanelX, self.logPanelY, self.logPanelW, self.logPanelH, 0.9, 0.18, 0.30, 0.42)

    local bridgeDetail = "PZLife global bridge unavailable"
    if self.statusText == "READY" then
        bridgeDetail = "PZLife global bridge attached"
    end
    self:drawText(bridgeDetail, self.logPanelX + 8, self.logPanelY + 8, 0.55, 0.85, 0.95, 1.0, FONT_SMALL)

    local y = self.logPanelY + 8 + FONT_HGT_SMALL + 6
    local startIndex = math.max(1, #self.lines - 14)
    for i = startIndex, #self.lines do
        self:drawText(self.lines[i], self.logPanelX + 8, y, 0.86, 0.86, 0.86, 1.0, FONT_SMALL)
        y = y + FONT_HGT_SMALL + 2
        if y > self.logPanelY + self.logPanelH - FONT_HGT_SMALL - 4 then
            break
        end
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
    ui:pushLine("using pzlife global invoke path")
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
    if hasGlobalBridge() then
        logLine("PZLife global bridge ready")
    else
        local status = getBridgeStatus()
        logLine("PZLife global bridge pending: present=" .. tostring(status.present)
            .. " hasPzlife=" .. tostring(status.hasPzlife)
            .. " hasHas=" .. tostring(status.hasHas)
            .. " hasInvoke0=" .. tostring(status.hasInvoke0)
            .. " hasInvoke1=" .. tostring(status.hasInvoke1)
            .. " hasInvoke2=" .. tostring(status.hasInvoke2)
            .. " hasInvoke3=" .. tostring(status.hasInvoke3))
    end
end

Events.OnKeyPressed.Add(onKeyPressed)
Events.OnGameStart.Add(onGameStart)
