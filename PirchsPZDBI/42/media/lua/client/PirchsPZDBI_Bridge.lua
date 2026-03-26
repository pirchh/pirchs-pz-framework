local PREFIX = "[PZLIFE][LUA][bridge] "

local function log(msg)
    print(PREFIX .. tostring(msg))
end

log("PirchsPZDBI_Bridge.lua loaded")

pz = pz or {}
pz.bridge = pz.bridge or {}

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

local function normalize_error(ok, result)
    if ok then
        return result
    end
    return { ok = false, error = tostring(result) }
end

local function invoke0(method)
    return normalize_error(pcall(pzlife_invoke0, method))
end

local function invoke1(method, a1)
    return normalize_error(pcall(pzlife_invoke1, method, a1))
end

local function invoke2(method, a1, a2)
    return normalize_error(pcall(pzlife_invoke2, method, a1, a2))
end

local function invoke3(method, a1, a2, a3)
    return normalize_error(pcall(pzlife_invoke3, method, a1, a2, a3))
end

function pz.bridge.isAvailable()
    return bridge_ready()
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
        return nil
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
    else
        return { ok = false, error = "pz.bridge.invoke currently supports up to 3 args" }
    end
end

pz.bridge.debug = pz.bridge.debug or {}

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

for lua_name, method_name in pairs(debug_methods) do
    pz.bridge.debug[lua_name] = function(...)
        return pz.bridge.invoke(method_name, ...)
    end
end

local attempts = 0
local announced_ready = false

local function on_tick()
    attempts = attempts + 1
    if bridge_ready() then
        if not announced_ready then
            announced_ready = true
            local status = "ready"
            if has_global("pzlife_bridge_status") then
                local ok, value = pcall(pzlife_bridge_status)
                if ok then
                    status = tostring(value)
                end
            end
            log("global bridge ready after attempts=" .. tostring(attempts) .. " status=" .. status)
        end
        Events.OnTick.Remove(on_tick)
        return
    end

    if attempts == 1 or attempts % 60 == 0 then
        log("waiting for PZLife global bridge functions... attempts=" .. tostring(attempts))
    end
end

Events.OnTick.Add(on_tick)
