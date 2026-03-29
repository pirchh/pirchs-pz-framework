print("[PZLIFE][LUA][wallet] PirchsPZDBI_WalletRules.lua loaded")

function PirchsPZDBI_AcceptWalletItem(...)
    local item = select(2, ...)
    if item == nil then
        item = select(1, ...)
    end

    if item == nil or type(item.getFullType) ~= "function" then
        return false
    end

    return item:getFullType() == "PirchsPZDBI.PirchCash"
end
