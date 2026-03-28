print("[PZLIFE][LUA][bridge] PirchsPZDBI_Bridge.lua loaded")

pz = pz or {}
pz.bridge = pz.bridge or {}
pz.bridge.debug = pz.bridge.debug or {}

local bridgeReady = false
local attempts = 0

local debug_methods = {
    isLocalIdentityReady = "pz.bridge.debug.isLocalIdentityReady",
    localSnapshot = "pz.bridge.debug.localSnapshot",
    selfTestNow = "pz.bridge.debug.selfTestNow",
    selfTestStatus = "pz.bridge.debug.selfTestStatus",
    runSmokeSuite = "pz.bridge.debug.runSmokeSuite",
    resetLifecycle = "pz.bridge.debug.resetLifecycle",
    claimNode = "pz.bridge.debug.claimNode",
    releaseNode = "pz.bridge.debug.releaseNode",
    getNodeOwner = "pz.bridge.debug.getNodeOwner",
    listOwnedNodes = "pz.bridge.debug.listOwnedNodes",
    grantPermissionToSelf = "pz.bridge.debug.grantPermissionToSelf",
    revokePermissionFromSelf = "pz.bridge.debug.revokePermissionFromSelf",
    hasPermission = "pz.bridge.debug.hasPermission",
    explainPermission = "pz.bridge.debug.explainPermission",
    listPermissions = "pz.bridge.debug.listPermissions",
    assignRoleToSelf = "pz.bridge.debug.assignRoleToSelf",
    revokeRoleFromSelf = "pz.bridge.debug.revokeRoleFromSelf",
    hasRole = "pz.bridge.debug.hasRole",
    listRoles = "pz.bridge.debug.listRoles",
    listAvailableMethods = "pz.bridge.debug.listAvailableMethods",
    bridgeSnapshot = "pz.bridge.debug.bridgeSnapshot",
    getBalance = "pz.bridge.debug.getBalance",
    bankSnapshot = "pz.bridge.debug.bankSnapshot",
    getCarriedMoney = "pz.bridge.debug.getCarriedMoney",
    depositSelf = "pz.bridge.debug.depositSelf",
    withdrawSelf = "pz.bridge.debug.withdrawSelf",
    depositCarriedMoneySelf = "pz.bridge.debug.depositCarriedMoneySelf",
    depositAllCarriedMoneySelf = "pz.bridge.debug.depositAllCarriedMoneySelf",
    withdrawCashToInventorySelf = "pz.bridge.debug.withdrawCashToInventorySelf",
}

local function hasGlobalBridge()
    return type(pzlife) == "function"
        and type(pzlife_has) == "function"
        and type(pzlife_invoke0) == "function"
        and type(pzlife_invoke1) == "function"
        and type(pzlife_invoke2) == "function"
        and type(pzlife_invoke3) == "function"
end

local function getBridgeStatus()
    return {
        present = hasGlobalBridge(),
        debugBridgeEnabled = type(pzlife_has) == "function",
        hasInvoke0 = type(pzlife_invoke0) == "function",
        hasInvoke1 = type(pzlife_invoke1) == "function",
        hasInvoke2 = type(pzlife_invoke2) == "function",
        hasInvoke3 = type(pzlife_invoke3) == "function",
    }
end

local function invokeMethod(methodName, ...)
    if not hasGlobalBridge() then
        return false, "PZLife global bridge unavailable"
    end

    local argc = select("#", ...)
    local ok, result

    if argc == 0 then
        ok, result = pcall(function() return pzlife_invoke0(methodName) end)
    elseif argc == 1 then
        local a1 = ...
        ok, result = pcall(function() return pzlife_invoke1(methodName, a1) end)
    elseif argc == 2 then
        local a1, a2 = ...
        ok, result = pcall(function() return pzlife_invoke2(methodName, a1, a2) end)
    elseif argc == 3 then
        local a1, a2, a3 = ...
        ok, result = pcall(function() return pzlife_invoke3(methodName, a1, a2, a3) end)
    else
        return false, "Too many bridge arguments: " .. tostring(argc)
    end

    if not ok then
        return false, tostring(result)
    end

    return true, result
end

function pz.bridge.invoke(methodName, ...)
    return invokeMethod(methodName, ...)
end

local function attachDebugWrappers()
    for key, methodName in pairs(debug_methods) do
        pz.bridge.debug[key] = function(...)
            return invokeMethod(methodName, ...)
        end
    end
end

local function markReady()
    if bridgeReady then
        return
    end
    bridgeReady = true
    attachDebugWrappers()

    local status = getBridgeStatus()
    print("[PZLIFE][LUA][bridge] global bridge ready after attempts=" .. tostring(attempts)
        .. " status={present=" .. tostring(status.present)
        .. ", debugBridgeEnabled=" .. tostring(status.debugBridgeEnabled)
        .. ", hasInvoke0=" .. tostring(status.hasInvoke0)
        .. ", hasInvoke1=" .. tostring(status.hasInvoke1)
        .. ", hasInvoke2=" .. tostring(status.hasInvoke2)
        .. ", hasInvoke3=" .. tostring(status.hasInvoke3) .. "}")
end

local function onTick()
    if bridgeReady then
        return
    end

    attempts = attempts + 1
    if hasGlobalBridge() then
        markReady()
    end
end

Events.OnTick.Add(onTick)
