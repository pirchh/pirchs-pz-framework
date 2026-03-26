local PREFIX = "[PZLIFE][LUA][bridge] "

local function log(msg)
    print(PREFIX .. tostring(msg))
end

log("PirchsPZDBI_Bridge.lua loaded")

pz = pz or {}
pz.bridge = pz.bridge or {}
pz.bridge.debug = pz.bridge.debug or {}

local MAX_INVOKE_ARGS = 3
local attempts = 0
local announced_ready = false
local announced_timeout = false
local max_wait_attempts = 900

local function has_global(name)
    return type(_G[name]) == "function"
end

local function bridge_ready()
    return has_global("pzlife")
        and has_global("pzlife_has")
        and has_global("pzlife_invoke0")
        and has_global("pzlife_invoke1")
        and has_global("pzlife_invoke2")
        and has_global("pzlife_invoke3")
end

local function bridge_status_text()
    if has_global("pzlife_bridge_status") then
        local ok, value = pcall(pzlife_bridge_status)
        if ok then
            return tostring(value)
        end
        return "status-call-failed: " .. tostring(value)
    end
    return bridge_ready() and "ready" or "pending"
end

local function unavailable_result(message)
    return {
        ok = false,
        error = tostring(message),
        bridgeReady = bridge_ready(),
        bridgeStatus = bridge_status_text(),
    }
end

local function normalize_call(ok, result)
    if ok then
        return result
    end
    return unavailable_result(result)
end

local function invoke0(method)
    return normalize_call(pcall(pzlife_invoke0, method))
end

local function invoke1(method, a1)
    return normalize_call(pcall(pzlife_invoke1, method, a1))
end

local function invoke2(method, a1, a2)
    return normalize_call(pcall(pzlife_invoke2, method, a1, a2))
end

local function invoke3(method, a1, a2, a3)
    return normalize_call(pcall(pzlife_invoke3, method, a1, a2, a3))
end

function pz.bridge.isAvailable()
    return bridge_ready()
end

function pz.bridge.status()
    return {
        available = bridge_ready(),
        status = bridge_status_text(),
        attempts = attempts,
        hasPzlife = has_global("pzlife"),
        hasHas = has_global("pzlife_has"),
        hasInvoke0 = has_global("pzlife_invoke0"),
        hasInvoke1 = has_global("pzlife_invoke1"),
        hasInvoke2 = has_global("pzlife_invoke2"),
        hasInvoke3 = has_global("pzlife_invoke3"),
    }
end

function pz.bridge.hasMethod(method)
    if not bridge_ready() then
        return false
    end
    local ok, result = pcall(pzlife_has, method)
    return ok and result == true
end

function pz.bridge.invoke(method, ...)
    if not bridge_ready() then
        return unavailable_result("PZLife global bridge unavailable")
    end

    local argc = select("#", ...)
    if argc == 0 then
        return invoke0(method)
    elseif argc == 1 then
        return invoke1(method, select(1, ...))
    elseif argc == 2 then
        return invoke2(method, select(1, ...), select(2, ...))
    elseif argc == 3 then
        return invoke3(method, select(1, ...), select(2, ...), select(3, ...))
    end

    return unavailable_result("pz.bridge.invoke currently supports up to " .. tostring(MAX_INVOKE_ARGS) .. " args")
end

function pz.bridge.invokeIfPresent(method, ...)
    if not pz.bridge.hasMethod(method) then
        return unavailable_result("Method not exposed: " .. tostring(method))
    end
    return pz.bridge.invoke(method, ...)
end

function pz.bridge.debug.isAvailable()
    return pz.bridge.isAvailable() and pz.bridge.hasMethod("pz.bridge.debug.isLocalIdentityReady")
end

local debug_methods = {
    isLocalIdentityReady = "pz.bridge.debug.isLocalIdentityReady",
    selfTestNow = "pz.bridge.debug.selfTestNow",
    selfTestStatus = "pz.bridge.debug.selfTestStatus",
    runSmokeSuite = "pz.bridge.debug.runSmokeSuite",
    resetLifecycle = "pz.bridge.debug.resetLifecycle",
    localSnapshot = "pz.bridge.debug.localSnapshot",
    listAvailableMethods = "pz.bridge.debug.listAvailableMethods",
    bridgeSnapshot = "pz.bridge.debug.bridgeSnapshot",
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
    listRoles = "pz.bridge.debug.listRoles"
}

function pz.bridge.debug.methodName(luaName)
    return debug_methods[luaName]
end

function pz.bridge.debug.supportedMethodNames()
    local available = {}
    for lua_name, method_name in pairs(debug_methods) do
        if pz.bridge.hasMethod(method_name) then
            available[lua_name] = method_name
        end
    end
    return available
end

for lua_name, method_name in pairs(debug_methods) do
    pz.bridge.debug[lua_name] = function(...)
        return pz.bridge.invokeIfPresent(method_name, ...)
    end
end

local function on_tick()
    attempts = attempts + 1

    if bridge_ready() then
        if not announced_ready then
            announced_ready = true
            log("global bridge ready after attempts=" .. tostring(attempts) .. " status=" .. bridge_status_text())
        end
        Events.OnTick.Remove(on_tick)
        return
    end

    if attempts == 1 or attempts % 60 == 0 then
        log("waiting for PZLife global bridge functions... attempts=" .. tostring(attempts))
    end

    if attempts >= max_wait_attempts and not announced_timeout then
        announced_timeout = true
        log("bridge still unavailable after attempts=" .. tostring(attempts) .. " status=" .. bridge_status_text())
        Events.OnTick.Remove(on_tick)
    end
end

Events.OnTick.Add(on_tick)
