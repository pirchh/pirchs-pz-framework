print("[PZLIFE][LUA][client] PirchsPZDBI_IdentityReady.lua loaded")

local resolved = false

local function resolve(player)
    if resolved then return end
    if not player then player = getPlayer() end
    if not player then return end

    local playerNum = 0
    if player.getPlayerNum then
        playerNum = player:getPlayerNum()
    end

    print("[PZLIFE][LUA][client] resolving identity playerNum=" .. tostring(playerNum))

    if not isClient() then
        pirch.pz.debug.IdentityLifecycleBridge.onLocalPlayerCreated(playerNum, player)
        resolved = true
        return
    end

    sendClientCommand("PirchsPZDBI", "PlayerReady", { playerNum = playerNum })
    resolved = true
end

Events.OnPlayerUpdate.Add(resolve)
