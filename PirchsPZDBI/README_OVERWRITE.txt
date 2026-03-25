PirchsPZDBI overwrite contents

Included changes:
- 42/media/lua/client/PirchsPZDBI_Debug.lua
  Replaces the one-shot smoke trigger with a manual debug menu.
  Open it in game with F6.
- 42/mod.info
  Rewritten as proper multi-line key=value metadata.

Suggested overwrite target:
C:\Users\<you>\Zomboid\mods\PirchsPZDBI\

Notes:
- This menu assumes the Java debug bridge methods already exist under pz.bridge.debug.*
- Results go both to the in-menu log area and the console/log output.
- If a button errors, the menu will show the Lua-side pcall result so you can see which method/signature mismatched.
