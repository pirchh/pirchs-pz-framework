This overwrite pack adds a cleaner readiness handshake for debug testing.

What changed:
- Added pz.bridge.debug.isLocalIdentityReady
- Added pz.bridge.debug.runSmokeSuite(nodeKey, nodeType)
- Updated the Lua debug file to wait on Java-side readiness instead of guessing based on time

Why this is better:
- Java already owns local identity detection and resolution.
- Lua no longer tries to infer readiness by timing.
- Lua simply waits for Java to say the local account is ready, then runs the smoke suite once.

Expected flow:
1. Game boots.
2. Java identity lifecycle resolves the local account.
3. Lua sees pz.bridge.debug.isLocalIdentityReady() == true.
4. Lua runs:
   - localSnapshot
   - selfTestNow
   - runSmokeSuite(test:node_debug_1, node)

Suggested test steps:
- Rebuild and install to mod.
- Boot the game.
- Wait until the player fully loads into the world.
- Check logs for:
  - Java-side local identity is ready. Running smoke suite.
  - DBI DEBUG TEST START / END
