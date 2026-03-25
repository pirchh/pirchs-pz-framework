print("[PZLIFE][LUA][bridge] PirchsPZDBI_Bridge.lua loaded")

pz = pz or {}
pz.bridge = pz.bridge or {}
pz.bridge.util = pz.bridge.util or {}
pz.bridge.debug = pz.bridge.debug or {}

local MAX_EXPOSE_ATTEMPTS = 300
local exposeAttempts = 0
local exposureDone = false
local exposedLogged = false

local function hasFacade()
    return LuaBridgeFacade ~= nil
end

local function findExposureHook()
    if LuaExposureHook ~= nil then
        return LuaExposureHook
    end

    if pirch and pirch.pz and pirch.pz.lua and pirch.pz.lua.LuaExposureHook then
        return pirch.pz.lua.LuaExposureHook
    end

    return nil
end

local function tryExposeOnMainThread()
    if hasFacade() then
        return true
    end

    local hook = findExposureHook()
    if not hook or not hook.ensureBridgeExposed then
        return false
    end

    local ok, result = pcall(function()
        return hook:ensureBridgeExposed()
    end)

    if ok then
        return result == true or hasFacade()
    end

    print("[PZLIFE][LUA][bridge] main-thread expose call failed: " .. tostring(result))
    return false
end

local function hasMethod(methodName)
    if not hasFacade() then
        return false
    end
    return LuaBridgeFacade.hasMethod(methodName)
end

local function invoke(methodName, ...)
    if not hasFacade() then
        error("LuaBridgeFacade unavailable")
    end
    return LuaBridgeFacade.invoke(methodName, ...)
end

pz.bridge.util.hasMethod = hasMethod
pz.bridge.util.invoke = invoke
pz.bridge.util.hasFacade = hasFacade

pz.bridge.debug.isAvailable = function()
    if not hasFacade() then
        return false
    end
    return hasMethod("pz.bridge.debug.isLocalIdentityReady")
end

pz.bridge.debug.isLocalIdentityReady = function()
    return invoke("pz.bridge.debug.isLocalIdentityReady")
end

pz.bridge.debug.localSnapshot = function()
    return invoke("pz.bridge.debug.localSnapshot")
end

pz.bridge.debug.selfTestNow = function()
    return invoke("pz.bridge.debug.selfTestNow")
end

pz.bridge.debug.selfTestStatus = function()
    return invoke("pz.bridge.debug.selfTestStatus")
end

pz.bridge.debug.resetLifecycle = function()
    return invoke("pz.bridge.debug.resetLifecycle")
end

pz.bridge.debug.runSmokeSuite = function(nodeKey, nodeType)
    return invoke("pz.bridge.debug.runSmokeSuite", nodeKey, nodeType)
end

pz.bridge.debug.claimNode = function(nodeKey, nodeType)
    return invoke("pz.bridge.debug.claimNode", nodeKey, nodeType)
end

pz.bridge.debug.releaseNode = function(nodeKey)
    return invoke("pz.bridge.debug.releaseNode", nodeKey)
end

pz.bridge.debug.getNodeOwner = function(nodeKey)
    return invoke("pz.bridge.debug.getNodeOwner", nodeKey)
end

pz.bridge.debug.listOwnedNodes = function()
    return invoke("pz.bridge.debug.listOwnedNodes")
end

pz.bridge.debug.grantPermissionToSelf = function(permissionKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.grantPermissionToSelf", permissionKey, scopeType, scopeKey)
end

pz.bridge.debug.revokePermissionFromSelf = function(permissionKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.revokePermissionFromSelf", permissionKey, scopeType, scopeKey)
end

pz.bridge.debug.hasPermission = function(permissionKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.hasPermission", permissionKey, scopeType, scopeKey)
end

pz.bridge.debug.explainPermission = function(permissionKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.explainPermission", permissionKey, scopeType, scopeKey)
end

pz.bridge.debug.listPermissions = function()
    return invoke("pz.bridge.debug.listPermissions")
end

pz.bridge.debug.assignRoleToSelf = function(roleKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.assignRoleToSelf", roleKey, scopeType, scopeKey)
end

pz.bridge.debug.revokeRoleFromSelf = function(roleKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.revokeRoleFromSelf", roleKey, scopeType, scopeKey)
end

pz.bridge.debug.hasRole = function(roleKey, scopeType, scopeKey)
    return invoke("pz.bridge.debug.hasRole", roleKey, scopeType, scopeKey)
end

pz.bridge.debug.listRoles = function()
    return invoke("pz.bridge.debug.listRoles")
end

local function onGameStart()
    print("[PZLIFE][LUA][bridge] debug facade ready=" .. tostring(pz.bridge.debug.isAvailable()))
end

local function onTick()
    if exposureDone or hasFacade() then
        if hasFacade() and not exposedLogged then
            print("[PZLIFE][LUA][bridge] LuaBridgeFacade became available after attempts=" .. tostring(exposeAttempts))
            exposedLogged = true
        end
        exposureDone = hasFacade()
        return
    end

    exposeAttempts = exposeAttempts + 1
    if exposeAttempts == 1 or exposeAttempts % 60 == 0 then
        print("[PZLIFE][LUA][bridge] attempting main-thread exposure... attempts=" .. tostring(exposeAttempts))
    end

    local success = tryExposeOnMainThread()
    if success or hasFacade() then
        exposureDone = true
        if not exposedLogged then
            print("[PZLIFE][LUA][bridge] LuaBridgeFacade became available after attempts=" .. tostring(exposeAttempts))
            exposedLogged = true
        end
        return
    end

    if exposeAttempts >= MAX_EXPOSE_ATTEMPTS then
        exposureDone = true
        print("[PZLIFE][LUA][bridge] giving up on main-thread exposure after attempts=" .. tostring(exposeAttempts))
    end
end

Events.OnGameStart.Add(onGameStart)
Events.OnTick.Add(onTick)
