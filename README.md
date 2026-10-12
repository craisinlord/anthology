# Anthology

A library mod for Minecraft 1.21.1 (NeoForge and Fabric) with the engine systems behind
[Antarchy](https://www.curseforge.com/minecraft/mc-mods/antarchy), so other mods can use them:

- **Gravity:** flip any entity's gravity, with smooth camera, movement, collision and projectile handling
- **Time dilation:** slow down or speed up entities, in fields or per entity
- **Beams:** a server-driven beam with path and impact damage and optional terrain destruction
- **Multipart entities:** extra hitboxes for big mobs, with damage multipliers per part
- **See-through portals:** linked portal pairs you can walk, ride and shoot through, and see through live
- **Camera shake:** proximity and impact shakes, synced from the server
- **Cross-dimension travel:** safe-landing teleports between dimensions

On its own it adds three effects (Inverted, Dilated, Contracted), the
time-dilation field entity, and the `/gravity` and `/timedilation` commands. Everything else does nothing until a mod uses it.

### Where to register hooks

Hooks are plain static registries (thread-safe lists). Register them once during mod construction or common
setup. Register client hooks (anything in a `.client` package) only on the client.

### Calling side

All state changes are **server-side**. Anthology syncs gravity, time rates, fields and camera shakes to the
clients that track an entity, including players who start tracking it later.

## Gravity

Package `com.craisinlord.anthology.gravity`.

```java
GravityApi.setGravityDirection(entity, GravityDirection.UP, new GravityTransition(8));
GravityApi.setForcedGravityDirection(player, GravityDirection.UP);
GravityApi.clearForcedGravity(player);
boolean upsideDown = GravityApi.isGravityInverted(entity);
```

- **`GravityDirection`:** `DOWN` or `UP`.
- **`GravityTransition`:** how many ticks the camera and body take to rotate. `GravityTransition.INSTANT` skips
  the animation.
- **Forced gravity:** wins over regions and effects until you clear it. Use it for scripted flips.
- **Vectors:** `getWorldVelocity`/`setWorldVelocity` read and write motion in world space, whichever way the
  entity is facing.
- **Eye and aim helpers:** `getEyeOffset`, `eyeY` and the `rangedBodyTarget*`/`rangedEyeTarget*` helpers fix
  your mob AI's aim when its target is inverted.

What's handled for you while inverted:

- Movement, jumping, falling and fall damage.
- Bounding boxes, eye height and camera rotation.
- Pathfinding, riding, minecarts and buckets.
- Bubble columns and explosions.
- Arrows, thrown projectiles and crossbows.
- Item drops.
- Compatibility with Alex's Caves, Ars Nouveau, Iron's Spells and Scorched Guns, each loaded only when that mod
  is installed.

The **`anthology:inverted`** effect flips the entity while it lasts.

### Hooks (`GravityHooks`)

| Hook | Use |
| --- | --- |
| `addInvertedRegion(Predicate<Entity>)` | Areas where gravity points up, such as Antarchy's Underside below a set Y. Projectiles and items in the region fall upward too. |
| `addInvertedFluid(Predicate<FluidState>)` | Fluids that push upward. |
| `addLoadListener(Consumer<Entity>)` | Runs when an entity's saved gravity loads. Use it to clean up stale state. |
| `addCameraRollSource(CameraRollSource)` | Adds camera roll, in degrees, each frame. Client only. |

Ops can run `/gravity get|set <targets> up|down` and `/gravity clear <targets>` (permission level 2).

## Time dilation

Package `com.craisinlord.anthology.time`. A rate of `1.0` is normal speed: `0.25` is quarter speed and `2.0` is
double speed. Field rates run from `0.05` to `3.0`. Personal rates can go up to `20.0`.

```java
TimeDilationFieldEntity field = TimeDilationApi.createField(level, center, 8.0, 0.25, 200);
TimeDilationApi.createAttachedField(level, boss, 10.0, 0.3, 400);
TimeDilationApi.setPersonalRate(player, "mymod:haste", 1.5);
TimeDilationApi.clearPersonalRate(player, "mymod:haste");
double rate = TimeDilationApi.getRate(entity);
```

- **Fields:** sphere-shaped and visible (pass `visual = false` to hide them), with a soft edge. They scale
  everything inside, including movement, AI, animations, attack cooldowns, projectiles, item and arrow despawn
  timers, and sound pitch on the client.
- **Owner-anchored fields:** `field.configureOwnerAnchored(owner)` makes the field follow its owner and leave the
  owner unaffected.
- **Personal rates:** kept per source ID, so different features don't overwrite each other. Setting a rate of
  `1.0` removes it. `setRateCeiling` caps an entity's rate.
- **Your own timers:** use `consumeTick(entity, "key")` or `consumeTicks` each tick, and run your logic only
  when it says a tick passed. `scaleCooldownTicks` stretches a cooldown by the entity's rate.
- **Immunity:** add entity types to the `#anthology:time_dilation_immune` tag.
- **Effects:** `anthology:dilated` slows the entity and `anthology:contracted` speeds it up.

### Hooks (`TimeDilationHooks`)

| Hook | Use |
| --- | --- |
| `addRateSource(RateSource)` | Adds a rate from your own state. Antarchy's Temporal Tuner uses this. |
| `addTrackedPredicate(Predicate<Entity>)` | Tracks extra entities for rate syncing. |
| `addFieldExclusion(FieldExclusion)` | Keeps certain entities out of certain fields. |
| `addChronosphereLevelSource(ToIntFunction<LivingEntity>)` | Supplies an armor or enchantment level that grants a personal field. |

Implement **`TimeDilationAnchor`** on an entity to choose where a field attached to it is centred.

Ops can run these commands (permission level 2):
- `/timedilation create <center> <radius> <rate> <durationTicks>` creates a field at a position.
- `/timedilation create_here <radius> <rate> <durationTicks>` creates one where you stand.
- `/timedilation list` lists fields.
- `/timedilation clear` removes them.

A duration of `-1` lasts until cleared. Command rates run from `0.05` to `1.0`.

## Beams

Package `com.craisinlord.anthology.beam`. A `BeamController` belongs to one living entity and is ticked on both
sides.

```java
private final BeamController beam = new BeamController(this);

beam.start(eyePos, targetPos, settings.durationTicks());

beam.tick(eyePos, getLookAngle(), target, SETTINGS, BeamTerrainMode.DESTROY, end -> this.entityData.set(BEAM_END, ...));
```

- **`BeamSettings`:** a record holding range, how fast the beam tracks its target, travel time, path and impact
  damage radii, damage, knockback, and terrain settings (interval, radii, mutations per tick, chances, max block
  resistance). It also has a line-of-sight flag.
- **`BeamTerrainMode`:** `NONE`, `DESTROY`, or `BUILD_ICE`. Terrain is only changed when the `mobGriefing`
  gamerule is on.
- **Damage:** samples the whole beam line. A target is hit when any part of its hitbox is within the radius, so
  very large mobs take hits wherever you aim.
- **Syncing:** the callback receives the end point each tick (or `null` when the beam stops). Sync it to clients
  however you render.
- **`BeamOwner`:** implement it to scale damage (`scaleBeamDamage`) and pick who can be hit (`canBeamDamage`).
  Owners never hit themselves or their allies. Mobs never hit their own type.
- **Rendering:** `beam.client.BeamRenderTypes` has the additive render types Antarchy draws its beams with.

## Multipart entities

Package `com.craisinlord.anthology.multipart`. Implement `MultipartEntityOwner` on your entity and keep the parts
array in a field:

```java
public class Colossus extends Monster implements MultipartEntityOwner {
    private static final MultipartLayout LAYOUT = () -> new MultipartPartDefinition[] {
            new MultipartPartDefinition("head", 3.0F, 3.0F, 2.0F, 4.0D, 8.0D, 0.0D, true),
            new MultipartPartDefinition("tail", 2.0F, 2.0F, 0.5F, -6.0D, 1.0D, 0.0D, false)
    };
    private Entity[] parts;

    public MultipartLayout anthology$getMultipartLayout() { return LAYOUT; }
    public Entity[] anthology$getMultipartParts() { return this.parts; }
    public void anthology$setMultipartParts(Entity[] parts) { this.parts = parts; }
}
```

- **Part definition:** name, width, height, damage multiplier, then forward, vertical and sideways offsets in
  the entity's own frame. The last value turns on collision.
- **Lifecycle:** Anthology spawns parts with the entity, moves them every tick (following yaw and gravity) and
  removes them with it.
- **Hits:** a hit on a part calls `anthology$hurtMultipartPart` with the damage already multiplied. The default
  forwards it to the entity's own `hurt`. Override it for weak points or armour.
- **Interaction:** works the same way through `anthology$interactMultipartPart`.
- **Loaders:** NeoForge uses `PartEntity`. Fabric uses a real `anthology:multipart_part` entity. Client hits are
  checked on the server before they're applied.

## See-through portals

Package `com.craisinlord.anthology.portal`. Extend `PortalEntity` and register the entity type yourself. The
saved data and the type ID stay yours.

```java
public class WarpPortal extends PortalEntity {
    public void place(Direction facing, BlockPos anchor) {
        this.anchor = anchor;
        this.setGeometry(facing, Direction.UP, 1, 2);
    }

    public PortalAppearance appearance() { return APPEARANCE; }
    protected boolean isStillValid(ServerLevel level) { return !level.getBlockState(this.anchor).isAir(); }
}

portalA.linkTo(portalB);
portalB.linkTo(portalA);
```

`linkTo` is one-way, so link both portals.

`setGeometry(facing, upAxis, width, height)` sets which way the portal faces and its size in blocks.

- **Required:** `appearance()` gives the colour, rim texture and surface texture (fancy and fast). `isStillValid`
  is checked each tick, and the portal closes when it returns false.
- **Optional overrides:**
  - `sounds()` (a `PortalSounds` record of enter, exit and ambient sounds).
  - `ambientPitch()`.
  - `canTransit(Entity)`.
  - `min/maxExitSpeed()`.
  - `tickBeforeTransit` (return `true` to skip transit this tick).
  - `clientTick()`.
  - `hasCustomView()`.
  - `saveExtra`/`loadExtra` for your own NBT.
- **Transit:** entities keep their momentum and exit facing the right way. Riders and passengers go through
  together, and so do projectiles and items. Sideways and upside-down portals work, as do inverted entities.
  Players cross with client-side prediction, so there is no rubber-banding. All of it stays within one dimension.
- **Rendering:** register `PortalEntityRenderer<YourPortal>` for your type. Live views use a stencil, with a
  framebuffer fallback. They can nest (portals seen through portals) and work with Sodium and Iris.

### Hooks

| Hook | Use |
| --- | --- |
| `PortalHooks.setSettings(PortalSettings)` | Global settings: see-through on/off, max live views, exit speeds and fancy surfaces. Hook these to your config. |
| `PortalHooks.addTransitExclusion(Predicate<Entity>)` | Entities that never cross, such as the projectile that creates portals. |
| `PortalHooks.addTransitListener(TransitListener)` | Called after any entity crosses. |
| `PortalClientHooks.registerCustomView(Predicate<PortalEntity>, CustomView)` | Draws something other than the linked portal's view. Antarchy uses this for portals to the moon. |
| `PortalClientHooks.setOutlinePolicy(OutlinePolicy)` | Decides which portals a player sees outlined through walls. |

## Camera shake

```java
CameraShakeSync.sendNearby(serverLevel, impactPos, 0.8F, 20, 24.0F);

CameraShake.register(this, 32.0D, 0.6F, () -> this.isStomping());
CameraShake.addStrengthSource(cameraPos -> myBossNearbyStrength(cameraPos));
```

- **Server-triggered impacts** fade with distance and time.
- **Client-side sources** (`register` and `addStrengthSource`) shake continuously while active.
- **Combined result:** shakes stack into a single yaw, pitch and roll offset.

## Cross-dimension travel

```java
CrossDimensionTravelApi.transferToSafePosition(entity, destinationLevel, preferredPos);
Vec3 spot = CrossDimensionTravelApi.findSafePosition(entity, destinationLevel, preferredPos);
```

It searches up to 8 blocks around the preferred position for a safe spot with room to stand and no fluid, using
only chunks that are already loaded. It then moves the entity there and sets its portal cooldown. Make sure the
target area is loaded first, for example with a chunk ticket. If no spot is found, it returns `false` (or `null`)
and the entity stays put. The entity arrives with no momentum.

## Networking helper

`AnthologyNetwork.registerClientbound`/`registerServerbound` registers a payload once from common code, and
Anthology wires it up on each loader. You may use it for your own payloads. Server-bound handlers run on the
server thread.

## Old Antarchy IDs

Worlds made with Antarchy before 2.2.2 keep working. `antarchy:inverted`, `antarchy:dilated`,
`antarchy:contracted` and `antarchy:time_dilation_field` are aliases of the `anthology:` IDs, and Anthology reads
the old NBT keys too.

## License

All Rights Reserved.
