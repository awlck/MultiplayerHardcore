Rather than banning a player when they die (like the Vanilla implementation of hardcore does),
create a new world and force everyone to start over. Inspired by [this Reddit post](https://www.reddit.com/r/Minecraft/comments/lp8jjm/we_played_a_multiplayer_hardcore_realm_during/).

**Built for**: Minecraft (Spigot) 1.16.4

## Features
* Create a new set of worlds (Overworld, Nether, End) every time a player dies, and move all players
there (clearing out inventories and resetting expecience)
* Retain past attempts in case someone decided to build some crazy stuff you may want to revisit later
* Customise death messages
* Keep track of death causes for each player (although there is no convenient way of displaying this
yet)
* Close the server on a schedule (i.e. only allow players onto the server at certain times) to ensure
nobody grinds away every night and beats the game single-handedly

Release downloads coming soon(tm). Development builds may be available from
[GitHub Actions](https://github.com/awlck/MultiplayerHardcore/actions/workflows/maven.yml).

[How to configure](https://github.com/awlck/MultiplayerHardcore#config)
