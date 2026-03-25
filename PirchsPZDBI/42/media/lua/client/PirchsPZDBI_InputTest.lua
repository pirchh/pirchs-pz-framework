print("[PZLIFE][INPUTTEST] script loaded")

local listenersAttached = false

local function onKeyPressed(key)
    print("[PZLIFE][INPUTTEST] OnKeyPressed key=" .. tostring(key))
end

local function onKeyStartPressed(key)
    print("[PZLIFE][INPUTTEST] OnKeyStartPressed key=" .. tostring(key))
end

local function onMouseDown(x, y)
    print("[PZLIFE][INPUTTEST] OnMouseDown x=" .. tostring(x) .. " y=" .. tostring(y))
end

local function attachListeners()
    if listenersAttached then
        print("[PZLIFE][INPUTTEST] listeners already attached")
        return
    end

    Events.OnKeyPressed.Add(onKeyPressed)
    Events.OnKeyStartPressed.Add(onKeyStartPressed)
    Events.OnMouseDown.Add(onMouseDown)
    listenersAttached = true

    print("[PZLIFE][INPUTTEST] listeners attached")
end

attachListeners()
