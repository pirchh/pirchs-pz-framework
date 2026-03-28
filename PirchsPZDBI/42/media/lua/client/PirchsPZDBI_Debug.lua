require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISTextEntryBox"
require "ISUI/ISLabel"

print("[PZLIFE][LUA][debug] PirchsPZDBI_Debug.lua loaded")

local FONT_SMALL = UIFont.Small
local FONT_MEDIUM = UIFont.Medium
local FONT_LARGE = UIFont.Large
local FONT_HGT_SMALL = getTextManager():getFontHeight(FONT_SMALL)
local FONT_HGT_MEDIUM = getTextManager():getFontHeight(FONT_MEDIUM)
local HOTKEY = (Keyboard and Keyboard.KEY_F6) or 64
local MENU = nil

local TAB_ORDER = { "Identity", "Nodes", "Permissions", "Roles", "Banking", "Utility" }

local TAB_ACTIONS = {
    Identity = {
        { id = "READY", label = "Identity Ready" },
        { id = "SNAPSHOT", label = "Local Snapshot" },
        { id = "SELF_TEST_NOW", label = "Self Test Now" },
        { id = "SELF_TEST_STATUS", label = "Self Test Status" },
        { id = "RESET_LIFECYCLE", label = "Reset Lifecycle" },
        { id = "RUN_SMOKE", label = "Run Smoke Suite" },
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
    Banking = {
        { id = "BANK_SNAPSHOT", label = "Bank Snapshot" },
        { id = "BANK_GET_BALANCE", label = "Get Balance" },
        { id = "BANK_DEPOSIT", label = "Deposit Amount" },
        { id = "BANK_WITHDRAW", label = "Withdraw Amount" },
    },
    Utility = {
        { id = "BRIDGE_SNAPSHOT", label = "Bridge Snapshot" },
        { id = "LIST_METHODS", label = "List Methods" },
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
    LIST_METHODS = "pz.bridge.debug.listAvailableMethods",
    BRIDGE_SNAPSHOT = "pz.bridge.debug.bridgeSnapshot",
    BANK_SNAPSHOT = "pz.bridge.debug.bankSnapshot",
    BANK_GET_BALANCE = "pz.bridge.debug.getBalance",
    BANK_DEPOSIT = "pz.bridge.debug.depositSelf",
    BANK_WITHDRAW = "pz.bridge.debug.withdrawSelf",
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

local function safeAmount(widget, fallback)
    local raw = safeText(widget, tostring(fallback or "100"))
    local amount = tonumber(raw)
    if amount == nil then
        return nil, "amount must be numeric"
    end
    amount = math.floor(amount)
    if amount <= 0 then
        return nil, "amount must be greater than zero"
    end
    return tostring(amount), nil
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
    o.childrenBuilt = false
    o.tabButtons = {}
    o.actionButtons = {}
    o.formRows = {}
    o.bridgeStatusSnapshot = {}

    o.titleBarHeight = 56
    o.sidebarWidth = 240
    o.sectionGap = 14
    o.pad = 14
    o.minActionHeight = 170
    o.maxActionHeight = 290
    o.resultHeight = 150

    o.nodeKeyEntry = nil
    o.nodeTypeEntry = nil
    o.permissionEntry = nil
    o.roleEntry = nil
    o.amountEntry = nil

    o.sidebarRect = {}
    o.formRect = {}
    o.actionRect = {}
    o.resultRect = {}
    o.logRect = {}

    return o
end

function PirchsPZDBIDebugMenu:pushLine(text)
    self.lines[#self.lines + 1] = tostring(text)
    while #self.lines > 250 do
        table.remove(self.lines, 1)
    end
end

function PirchsPZDBIDebugMenu:updateWrappedResult(text)
    local source = tostring(text or "")
    local wrapWidth = math.max(260, (self.resultRect.w or 500) - 24)
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
    self.lastResult = tostring(value)
    self:updateWrappedResult(self.lastResult)

    if ok then
        self:pushLine(tostring(label) .. " => ok=true value=" .. clip(self.lastResult, 220))
        log(tostring(label) .. " => ok=true value=" .. clip(self.lastResult, 220))
    else
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

function PirchsPZDBIDebugMenu:createTabButtons()
    for i = 1, #TAB_ORDER do
        local name = TAB_ORDER[i]
        local btn = ISButton:new(0, 0, 10, 10, name, self, function(target, button)
            target.activeTab = button.internal
            target:updateLayout()
        end)
        btn.internal = name
        btn:initialise()
        self:addChild(btn)
        self.tabButtons[#self.tabButtons + 1] = btn
    end
end

function PirchsPZDBIDebugMenu:createInputRow(key, labelText, defaultValue)
    local label = ISLabel:new(0, 0, FONT_HGT_SMALL, labelText, 1, 1, 1, 1, FONT_SMALL, true)
    label:initialise()
    self:addChild(label)

    local entry = ISTextEntryBox:new(tostring(defaultValue), 0, 0, 10, 10)
    entry:initialise()
    self:addChild(entry)

    self.formRows[#self.formRows + 1] = { key = key, label = label, entry = entry }

    if key == "nodeKey" then self.nodeKeyEntry = entry end
    if key == "nodeType" then self.nodeTypeEntry = entry end
    if key == "permission" then self.permissionEntry = entry end
    if key == "role" then self.roleEntry = entry end
    if key == "amount" then self.amountEntry = entry end
end

function PirchsPZDBIDebugMenu:createActionButtons()
    for t = 1, #TAB_ORDER do
        local tabName = TAB_ORDER[t]
        local list = TAB_ACTIONS[tabName]
        local bucket = {}

        for i = 1, #list do
            local action = list[i]
            local btn = ISButton:new(0, 0, 10, 10, action.label, self, self.onActionClicked)
            btn.internal = action.id
            btn.tabName = tabName
            btn:initialise()
            self:addChild(btn)
            bucket[#bucket + 1] = btn
        end

        self.actionButtons[tabName] = bucket
    end
end

function PirchsPZDBIDebugMenu:createChildren()
    if self.childrenBuilt then
        return
    end

    self:createTabButtons()
    self:createInputRow("nodeKey", "Node Key", "test:node_debug_1")
    self:createInputRow("nodeType", "Node Type", "node")
    self:createInputRow("permission", "Permission", "debug.use")
    self:createInputRow("role", "Role", "owner")
    self:createInputRow("amount", "Bank Amount", "100")
    self:createActionButtons()
    self:updateBridgeStatus()
    self.childrenBuilt = true
end

function PirchsPZDBIDebugMenu:getActionPanelHeight()
    local buttons = self.actionButtons[self.activeTab] or {}
    local buttonCount = #buttons
    local rows = math.max(1, math.ceil(buttonCount / 2))
    local headerHeight = 44
    local buttonHeight = 32
    local buttonGapY = 10
    local bodyHeight = rows * buttonHeight + math.max(0, rows - 1) * buttonGapY
    local total = headerHeight + 12 + bodyHeight + 12
    return math.max(self.minActionHeight, math.min(self.maxActionHeight, total))
end

function PirchsPZDBIDebugMenu:updateLayout()
    local pad = self.pad
    local gap = self.sectionGap
    local titleBarH = self.titleBarHeight
    local sidebarW = self.sidebarWidth

    local contentY = titleBarH + pad
    local contentH = self.height - contentY - pad
    local actionHeight = self:getActionPanelHeight()

    self.sidebarRect.x = pad
    self.sidebarRect.y = contentY
    self.sidebarRect.w = sidebarW
    self.sidebarRect.h = 270

    self.formRect.x = pad
    self.formRect.y = self.sidebarRect.y + self.sidebarRect.h + gap
    self.formRect.w = sidebarW
    self.formRect.h = contentH - self.sidebarRect.h - gap

    self.actionRect.x = self.sidebarRect.x + sidebarW + gap
    self.actionRect.y = contentY
    self.actionRect.w = self.width - self.actionRect.x - pad
    self.actionRect.h = actionHeight

    self.resultRect.x = self.actionRect.x
    self.resultRect.y = self.actionRect.y + self.actionRect.h + gap
    self.resultRect.w = self.actionRect.w
    self.resultRect.h = self.resultHeight

    self.logRect.x = self.actionRect.x
    self.logRect.y = self.resultRect.y + self.resultRect.h + gap
    self.logRect.w = self.actionRect.w
    self.logRect.h = self.height - self.logRect.y - pad

    local tabX = self.sidebarRect.x + 10
    local tabY = self.sidebarRect.y + 30
    local tabW = self.sidebarRect.w - 20
    local tabH = 30
    local tabGap = 8

    for i = 1, #self.tabButtons do
        local btn = self.tabButtons[i]
        btn:setX(tabX)
        btn:setY(tabY + (i - 1) * (tabH + tabGap))
        btn:setWidth(tabW)
        btn:setHeight(tabH)
    end

    local rowStartY = self.formRect.y + 34
    local labelX = self.formRect.x + 10
    local entryX = self.formRect.x + 10
    local entryW = self.formRect.w - 20
    local labelToEntryGap = 8
    local rowGap = 18
    local entryH = 24
    local cursorY = rowStartY

    for i = 1, #self.formRows do
        local row = self.formRows[i]
        row.label:setX(labelX)
        row.label:setY(cursorY)
        row.entry:setX(entryX)
        row.entry:setY(cursorY + FONT_HGT_SMALL + labelToEntryGap)
        row.entry:setWidth(entryW)
        row.entry:setHeight(entryH)
        cursorY = cursorY + FONT_HGT_SMALL + labelToEntryGap + entryH + rowGap
    end

    local actionPad = 12
    local buttonGapX = 10
    local buttonGapY = 10
    local buttonW = math.floor((self.actionRect.w - (actionPad * 2) - buttonGapX) / 2)
    local buttonH = 32
    local startX = self.actionRect.x + actionPad
    local startY = self.actionRect.y + 40

    for tabName, buttons in pairs(self.actionButtons) do
        local visible = (tabName == self.activeTab)
        for i = 1, #buttons do
            local col = (i - 1) % 2
            local row = math.floor((i - 1) / 2)
            local btn = buttons[i]
            btn:setX(startX + col * (buttonW + buttonGapX))
            btn:setY(startY + row * (buttonH + buttonGapY))
            btn:setWidth(buttonW)
            btn:setHeight(buttonH)
            btn:setVisible(visible)
        end
    end

    self:updateWrappedResult(self.lastResult)
end

function PirchsPZDBIDebugMenu:initialise()
    ISPanel.initialise(self)
end

function PirchsPZDBIDebugMenu:onResize()
    ISPanel.onResize(self)
    self:updateLayout()
end

function PirchsPZDBIDebugMenu:onActionClicked(button)
    local nodeKey = safeText(self.nodeKeyEntry, "test:node_debug_1")
    local nodeType = safeText(self.nodeTypeEntry, "node")
    local permission = safeText(self.permissionEntry, "debug.use")
    local role = safeText(self.roleEntry, "owner")
    local amountValue, amountErr = safeAmount(self.amountEntry, "100")

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
        self:invokeById("RUN_SMOKE", nodeKey, nodeType)
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
    elseif button.internal == "BANK_SNAPSHOT" then
        self:invokeById("BANK_SNAPSHOT")
    elseif button.internal == "BANK_GET_BALANCE" then
        self:invokeById("BANK_GET_BALANCE")
    elseif button.internal == "BANK_DEPOSIT" then
        if amountErr then
            self:finishCall("BANK_DEPOSIT", false, amountErr)
            return
        end
        self:invokeById("BANK_DEPOSIT", amountValue)
    elseif button.internal == "BANK_WITHDRAW" then
        if amountErr then
            self:finishCall("BANK_WITHDRAW", false, amountErr)
            return
        end
        self:invokeById("BANK_WITHDRAW", amountValue)
    elseif button.internal == "LIST_METHODS" then
        self:invokeById("LIST_METHODS")
    elseif button.internal == "BRIDGE_SNAPSHOT" then
        self:invokeById("BRIDGE_SNAPSHOT")
    elseif button.internal == "REFRESH_STATUS" then
        self:updateBridgeStatus()
        if self.statusText == "READY" then
            self:pushLine("bridge status refreshed: PZLife global bridge ready")
        else
            self:pushLine("bridge status refreshed: PZLife global bridge unavailable")
        end
    elseif button.internal == "LOG_DEFAULTS" then
        self:pushLine("nodeKey=" .. nodeKey .. " | nodeType=" .. nodeType .. " | permission=" .. permission .. " | role=" .. role .. " | amount=" .. tostring(amountValue or "100"))
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

    self:drawRect(0, 0, self.width, self.titleBarHeight, 0.40, 0.00, 0.00, 0.00)
    self:drawRectBorder(0, 0, self.width, self.titleBarHeight, 0.95, 0.12, 0.35, 0.50)

    self:drawText("PZLife Debug Console", 14, 10, 1, 1, 1, 1, FONT_LARGE)
    self:drawText("F6 toggles the console", 14, 16 + FONT_HGT_MEDIUM, 0.72, 0.84, 0.94, 1.0, FONT_SMALL)
    self:drawText("Runtime: " .. self.statusText, self.width - 170, 18, self.statusColor.r, self.statusColor.g, self.statusColor.b, self.statusColor.a, FONT_MEDIUM)

    self:drawRect(self.sidebarRect.x, self.sidebarRect.y, self.sidebarRect.w, self.sidebarRect.h, 0.16, 0.05, 0.07, 0.09)
    self:drawRectBorder(self.sidebarRect.x, self.sidebarRect.y, self.sidebarRect.w, self.sidebarRect.h, 0.85, 0.15, 0.25, 0.35)
    self:drawText("Tabs", self.sidebarRect.x + 10, self.sidebarRect.y + 8, 0.88, 0.95, 1.0, 1.0, FONT_MEDIUM)

    for i = 1, #self.tabButtons do
        local btn = self.tabButtons[i]
        if btn.internal == self.activeTab then
            btn.backgroundColor = { r = 0.18, g = 0.48, b = 0.72, a = 0.95 }
        else
            btn.backgroundColor = { r = 0.10, g = 0.10, b = 0.12, a = 0.90 }
        end
    end

    self:drawRect(self.formRect.x, self.formRect.y, self.formRect.w, self.formRect.h, 0.16, 0.05, 0.07, 0.09)
    self:drawRectBorder(self.formRect.x, self.formRect.y, self.formRect.w, self.formRect.h, 0.85, 0.15, 0.25, 0.35)
    self:drawText("Inputs", self.formRect.x + 10, self.formRect.y + 8, 0.88, 0.95, 1.0, 1.0, FONT_MEDIUM)

    self:drawRect(self.actionRect.x, self.actionRect.y, self.actionRect.w, self.actionRect.h, 0.16, 0.05, 0.07, 0.09)
    self:drawRectBorder(self.actionRect.x, self.actionRect.y, self.actionRect.w, self.actionRect.h, 0.85, 0.15, 0.25, 0.35)
    self:drawText(self.activeTab .. " Actions", self.actionRect.x + 12, self.actionRect.y + 8, 0.88, 0.95, 1.0, 1.0, FONT_MEDIUM)

    self:drawRect(self.resultRect.x, self.resultRect.y, self.resultRect.w, self.resultRect.h, 0.16, 0.05, 0.07, 0.09)
    self:drawRectBorder(self.resultRect.x, self.resultRect.y, self.resultRect.w, self.resultRect.h, 0.85, 0.15, 0.25, 0.35)
    self:drawText("Last Result", self.resultRect.x + 12, self.resultRect.y + 8, 0.88, 0.95, 1.0, 1.0, FONT_MEDIUM)

    local resultY = self.resultRect.y + 30
    for i = 1, #self.lastResultWrapped do
        self:drawText(self.lastResultWrapped[i], self.resultRect.x + 12, resultY, 0.92, 0.92, 0.92, 1.0, FONT_SMALL)
        resultY = resultY + FONT_HGT_SMALL + 2
        if resultY > self.resultRect.y + self.resultRect.h - FONT_HGT_SMALL - 4 then
            break
        end
    end

    self:drawRect(self.logRect.x, self.logRect.y, self.logRect.w, self.logRect.h, 0.16, 0.05, 0.07, 0.09)
    self:drawRectBorder(self.logRect.x, self.logRect.y, self.logRect.w, self.logRect.h, 0.85, 0.15, 0.25, 0.35)
    self:drawText("Activity Log", self.logRect.x + 12, self.logRect.y + 8, 0.88, 0.95, 1.0, 1.0, FONT_MEDIUM)

    local bridgeDetail = "PZLife global bridge unavailable"
    if self.statusText == "READY" then
        bridgeDetail = "PZLife global bridge attached"
    end
    self:drawText(bridgeDetail, self.logRect.x + 12, self.logRect.y + 30, 0.55, 0.85, 0.95, 1.0, FONT_SMALL)

    local y = self.logRect.y + 30 + FONT_HGT_SMALL + 8
    local availableHeight = self.logRect.h - (y - self.logRect.y) - 8
    local maxLines = math.max(4, math.floor(availableHeight / (FONT_HGT_SMALL + 2)))
    local startIndex = math.max(1, #self.lines - maxLines + 1)

    for i = startIndex, #self.lines do
        self:drawText(self.lines[i], self.logRect.x + 12, y, 0.86, 0.86, 0.86, 1.0, FONT_SMALL)
        y = y + FONT_HGT_SMALL + 2
        if y > self.logRect.y + self.logRect.h - FONT_HGT_SMALL - 4 then
            break
        end
    end
end

local function createMenu()
    local width = 1080
    local height = 760
    local x = math.max(20, math.floor((getCore():getScreenWidth() - width) / 2))
    local y = math.max(20, math.floor((getCore():getScreenHeight() - height) / 2))
    local ui = PirchsPZDBIDebugMenu:new(x, y, width, height)
    ui:initialise()
    ui:instantiate()
    ui:createChildren()
    ui:updateLayout()
    ui:addToUIManager()
    ui:setVisible(true)
    ui:updateWrappedResult("idle")
    ui:pushLine("debug menu opened")
    ui:pushLine("layout pass: banking tab added + parent instantiate first + child attach after instantiate")
    ui:pushLine("nodeKey=test:node_debug_1 | nodeType=node | permission=debug.use | role=owner | amount=100")
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
