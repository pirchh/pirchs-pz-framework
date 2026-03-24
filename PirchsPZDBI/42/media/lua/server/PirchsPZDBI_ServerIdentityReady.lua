print("[PZLIFE][LUA][server] PirchsPZDBI_ServerIdentityReady.lua loaded")

local function onClientCommand(module, command, player, args)
    if module ~= "PirchsPZDBI" then return end
    if command ~= "PlayerReady" then return end

    print("[PZLIFE][LUA][server] PlayerReady received")

    if player then
        pirch.pz.debug.IdentityLifecycleBridge.onServerPlayerReady(player, module, command)
    end
end

Events.OnClientCommand.Add(onClientCommand)
