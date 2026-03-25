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

    if isClient() then
        sendClientCommand("PirchsPZDBI", "PlayerReady", { playerNum = playerNum })
    else
        print("[PZLIFE][LUA][client] single-player detected; Java-side detector will resolve local identity")
    end

    resolved = true
end

Events.OnPlayerUpdate.Add(resolve)
