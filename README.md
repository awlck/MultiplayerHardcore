# MultiplayerHardcore

A Minecraft (Spigot) plugin for collaborative hardcore. Rather than banning a player when they die
(like the Vanilla implementation of hardcore does), create a new world and force everyone to start
over. Inspired by [this Reddit post](https://www.reddit.com/r/Minecraft/comments/lp8jjm/we_played_a_multiplayer_hardcore_realm_during/).

**Note**: MultiplayerHardcore is intended to be used on a server of its own. Use BungeeCord
networking if you want to offer multiple game modes.

## Features
* Create a new set of worlds (Overworld, Nether, End) every time a player dies, and move all players
there (clearing out inventories and resetting expecience)
* Retain past attempts in case someone decided to build some crazy stuff you may want to revisit later
* Customise death messages
* Keep track of death causes for each player (although there is no convenient way of displaying this
yet)
* Close the server on a schedule (i.e. only allow players onto the server at certain times) to ensure
nobody grinds away every night and beats the game single-handedly

## Dependencies
* Multiverse-Core (4.2.2) (compile, run)
* Multiverse-NetherPortals (run)

**Built for**: Spigot 1.16.4

## Config
### `timeOfPlay`
Schedule for when players should be allowed onto the server. Possible values for each day are `open`
(all day), `closed` (all day), or a time span like `16:30-22:00`. If the ending time is before the
beginning, it is interpreted to be on the next day, i.e. writing `FRIDAY: 16:30-02:00` would open
the server starting fridays at 16:30hrs (4:30pm) until saturday at 02:00hrs (2am). All times are
interpreted as server local time.

### `messages`
Custom messages to display when a player dies. `%1$s` is replaced with the dying player, `%2$s` is
replaced with the attacker, if applicable (does not apply to the `ending` message).

For all message categories except `ending`, you can supply several messages. If you do, one will be
chosen at random every time.

* `ending`: suffix that is always appended to the specific death message.
* `general`: list of messages that are used if no messages are set for the specific cause of death.
* `ENTITY_ATTACK`, etc.: for each [damage cause](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html),
a list of messages to be used when a player dies of that cause.
  
For example, if the `messages` block in your config looks like this:

```yaml
messages:
  ending: "Your journey begins anew..."
  general:
    - "%1$s died."
  PROJECTILE:
    - "%1$s was shot to death by %2$s."
```

and a player called "SomeGuy" is killed (shot) by a skeleton, the death message would read:
> SomeGuy was shot to death by Skeleton. Your journey begins anew...

However, if they were killed by a Zombie, the message would use the more generic "SomeGuy died"
wording, since we didn't set a specific message for `ENTITY_ATTACK`.

### `attempt`
* `attemptsToKeep`:
  The number of old attempts to keep, in case you wish to revisit them. Set to a negative value to
  keep *all* old attempts. (Can cause significant memory and disk usage!) The default is 10, i.e.
  `attempt1` will be deleted when `attempt12` becomes active.
* `worldPrefix`:
The base name to use for worlds created by the plugin. The default is `attempt`, which means the
worlds will be called `attempt1`, `attempt2`, and so on. (Consequently, the Nether for the first
attempt will be called `attempt1_nether`, and the corresponding End will be called
`attempt1_the_end`.)
  
### `stats`
For each player (UUID), lists how often they have died of each cause. There is currently no way to
output this nicely.

## Commands and Permissions
MultiplayerHardcore does not currently have any commands or permissions to worry about. Everything
is handled by config file only.

## Caveats
### Memory and Disk Usage
If your server is running out of RAM or disk space to store old attempts, try reducing the number
of attempts that are retained (using `attempt.attemptsToKeep` in the config file). If you can't
launch the server to do this, delete the folders manually and remove the relevant blocks from
`plugins/Multiverse-Core/worlds.yml`

### Other Game Modes
MultiplayerHardcore is intended to be used on a server of its own. There is no way for players to
"opt out of" MultiplayerHardcore, and the plugin will react to deaths in all worlds. Use BungeeCord
networking if you want to offer several game modes.