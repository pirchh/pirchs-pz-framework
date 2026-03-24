print("[PZLIFE][LUA][debug] PirchsPZDBI_Debug.lua loaded")

local ran = false
local attempts = 0
local maxAttempts = 900

local function log(v)
    print("[PZLIFE][LUA][debug] " .. tostring(v))
end

local function isReadyValue(value)
    if value == true then
        return true
    end

    local text = tostring(value)
    return text == "true" or text == "True" or text == "1"
end

local function tryRun(player)
    if ran then
        Events.OnPlayerUpdate.Remove(tryRun)
        return
    end

    attempts = attempts + 1

    if player == nil then
        if attempts % 60 == 0 then
            log("waiting for player object... attempt=" .. tostring(attempts))
        end
        return
    end

    if pz == nil or pz.bridge == nil or pz.bridge.debug == nil then
        if attempts % 60 == 0 then
            log("bridge not ready yet... attempt=" .. tostring(attempts))
        end
        return
    end

    local okReady, ready = pcall(function()
        return pz.bridge.debug.isLocalIdentityReady()
    end)

    if not okReady then
        if attempts % 60 == 0 then
            log("isLocalIdentityReady failed: " .. tostring(ready))
        end
        return
    end

    if not isReadyValue(ready) then
        if attempts % 60 == 0 then
            log("local identity not ready yet... attempt=" .. tostring(attempts) .. " value=" .. tostring(ready))
        end
        if attempts >= maxAttempts then
            log("giving up waiting for local identity")
            Events.OnPlayerUpdate.Remove(tryRun)
        end
        return
    end

    ran = true

    log("local identity ready, running smoke suite")
    log("=== DBI DEBUG TEST START ===")

    local ok1, snapshot = pcall(function()
        return pz.bridge.debug.localSnapshot()
    end)
    log("localSnapshot ok=" .. tostring(ok1) .. " value=" .. tostring(snapshot))

    local ok2, selfTest = pcall(function()
        return pz.bridge.debug.selfTestNow()
    end)
    log("selfTestNow ok=" .. tostring(ok2) .. " value=" .. tostring(selfTest))

    local ok3, smoke = pcall(function()
        return pz.bridge.debug.runSmokeSuite("test:node_debug_1", "node")
    end)
    log("runSmokeSuite ok=" .. tostring(ok3) .. " value=" .. tostring(smoke))

    log("=== DBI DEBUG TEST END ===")

    Events.OnPlayerUpdate.Remove(tryRun)
end

Events.OnPlayerUpdate.Add(tryRun)
print("[PZLIFE][LUA][debug] one-shot smoke trigger armed")