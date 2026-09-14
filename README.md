# HyfeUtils

API Java para simplificar utilidades comunes de plugins Bukkit/Spigot/Paper.
No es un plugin instalable: se inicializa desde el plugin huésped y sus clases
deben sombrearse en el jar final.

## Requisitos

- Java 17.
- Servidor Bukkit compatible desde 1.8.8.
- ViaVersion recomendado para detectar la versión del cliente y entregar RGB.

## Uso

```java
public final class ExamplePlugin extends JavaPlugin {
    private HyfeUtils hyfe;

    @Override
    public void onEnable() {
        this.hyfe = HyfeUtils.create(this);
        hyfe.messages().send(getServer().getConsoleSender(), "&aPlugin activado");
    }

    @Override
    public void onDisable() {
        if (hyfe != null) {
            hyfe.close();
        }
    }
}
```

Los formatos `&a` y `&#RRGGBB` son compatibles. En clientes 1.16 o superiores
se utiliza RGB; en clientes antiguos se selecciona el color legacy más cercano.

## Comparación: sin HyfeUtils vs con HyfeUtils

### Mensajes con colores

**Sin HyfeUtils:**
```java
// Necesitas detectar si es Player para obtener protocolo
// y manejar colores manualmente
if (player instanceof Player) {
    Player p = (Player) player;
    int protocol = -1;
    try {
        Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
        Object api = via.getMethod("getAPI").invoke(null);
        Method method = api.getClass().getMethod("getPlayerVersion", UUID.class);
        protocol = (int) method.invoke(api, p.getUniqueId());
    } catch (Exception ignored) {}
    
    // Convertir colores manualmente...
    String msg = message.replace("&0", "\u00a70")
        .replace("&1", "\u00a71")
        .replace("&2", "\u00a72")
        // ... 15+ líneas más de reemplazos
        .replace("&#", "\u00a7x");
    
    p.sendMessage(msg);
}
```

**Con HyfeUtils:**
```java
hyfe.messages().send(player, "&aHola &#FF0000Mundo");
```

---

### Títulos

**Sin HyfeUtils:**
```java
// Necesitas verificar el protocolo del cliente
// y construir los componentes manualmente
int protocol = -1;
try {
    Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
    Object api = via.getMethod("getAPI").invoke(null);
    Method m = api.getClass().getMethod("getPlayerVersion", UUID.class);
    protocol = (int) m.invoke(api, player.getUniqueId());
} catch (Exception ignored) {}

String titleColorized = colorize("&#FFAA00Victoria", protocol);
String subColorized = colorize("&fHas ganado", protocol);

Component title = LegacyComponentSerializer.legacySection().deserialize(titleColorized);
Component subtitle = LegacyComponentSerializer.legacySection().deserialize(subColorized);

Title.Times times = Title.Times.times(
    Duration.ofMillis(200),
    Duration.ofMillis(1000),
    Duration.ofMillis(200)
);

player.showTitle(Title.title(title, subtitle, times));
```

**Con HyfeUtils:**
```java
hyfe.titles().send(player, "&#FFAA00Victoria", "&fHas ganado", 10, 40, 10);
```

---

### Textos clickeables

**Sin HyfeUtils (código manual con Adventure):**
```java
Component text = Component.text()
    .content("Click aquí")
    .color(NamedTextColor.GREEN)
    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/clan info"))
    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
        Component.text("Ver tu clan").color(NamedTextColor.YELLOW)))
    .build();

// También necesitas crear unAudience
BukkitAudiences audiences = BukkitAudiences.create(plugin);
audiences.player(player).sendMessage(text);
```

**Con HyfeUtils:**
```java
import static com.hyfecraft.hyfeutils.text.ClickableTextService.clickable;

hyfe.clickableText().send(player,
    clickable("&aClick aquí")
        .runCommand("/clan info")
        .hoverText("&eVer tu clan")
        .build()
);
```

---

### Títulos animados (efecto ola)

**Sin HyfeUtils:**
```java
String text = "¡COMIENZA!";
String baseColor = "\u00a76\u00a7l";
String waveColor = "\u00a7f\u00a7l";
String[] rainbow = {"\u00a7c", "\u00a76", "\u00a7e", "\u00a7a", "\u00a7b", "\u00a79", "\u00a7d"};

int[] offset = {0};
BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    if (!player.isOnline()) {
        cancel();
        return;
    }
    
    StringBuilder wave = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c == ' ') {
            wave.append(' ');
            continue;
        }
        boolean useWave = (i + offset[0]) % 2 == 0;
        wave.append(useWave ? waveColor : baseColor);
        wave.append(c);
    }
    
    Component main = LegacyComponentSerializer.legacySection()
        .deserialize(wave.toString());
    Component sub = Component.empty();
    Title.Times times = Title.Times.times(
        Duration.ZERO, Duration.ofMillis(100), Duration.ZERO);
    player.showTitle(Title.title(main, sub, times));
    
    offset[0]++;
}, 0L, 3L);
```

**Con HyfeUtils:**
```java
hyfe.animatedTitles().wave(player,
    "&6&l¡COMIENZA!",
    "&f&l",
    60, 3
);
```

---

### Mensajes centrados

**Sin HyfeUtils:**
```java
// Tabla de font metrics manual
int CHAT_WIDTH = 176;
Map<Character, Integer> widths = new HashMap<>();
widths.put('A', 6); widths.put('B', 6); // ... 90+ líneas de mapa
widths.put(' ', 4);

String message = colorize("&6&lMCLANS");
int msgWidth = 0;
for (char c : message.toCharArray()) {
    if (c == '\u00a7') continue;
    msgWidth += widths.getOrDefault(c, 6);
}

int padding = (CHAT_WIDTH - msgWidth) / 2;
StringBuilder centered = new StringBuilder("\u00a7r");
while (getStringWidth(centered.toString()) < padding) {
    centered.append(' ');
}
centered.append(message);
player.sendMessage(centered.toString());
```

**Con HyfeUtils:**
```java
hyfe.chat().sendCentered(player, "&6&lMCLANS");
```

---

### Eventos

**Sin HyfeUtils:**
```java
public class MyListener implements Listener {
    private final MyPlugin plugin;
    
    public MyListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(colorize("&aBienvenido " + player.getName()));
    }
}

// En onEnable:
getServer().getPluginManager().registerEvents(new MyListener(this), this);
```

**Con HyfeUtils:**
```java
hyfe.events().listen(PlayerJoinEvent.class,
    event -> hyfe.messages().send(event.getPlayer(), "&aBienvenido"));
```

---

### Comandos

**Sin HyfeUtils:**
```java
// En onEnable:
getCommand("clan").setExecutor(new CommandExecutor() {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(colorize("&aHola"));
        return true;
    }
});
getCommand("clan").setTabCompleter(new TabCompleter() {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Arrays.asList("create", "delete", "info");
    }
});
```

**Con HyfeUtils:**
```java
hyfe.commands().bind("clan",
    (sender, cmd, label, args) -> { hyfe.messages().send(sender, "&aHola"); return true; },
    (sender, cmd, alias, args) -> Arrays.asList("create", "delete", "info")
);
```

---

### Configuración

**Sin HyfeUtils:**
```java
// Cargar config
File dataFolder = getDataFolder();
if (!dataFolder.exists()) {
    dataFolder.mkdirs();
}
File configFile = new File(dataFolder, "config.yml");
if (!configFile.exists()) {
    saveResource("config.yml", false);
}
FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
String message = config.getString("messages.welcome", "&aBienvenido");

// Guardar config
try {
    config.save(configFile);
} catch (IOException e) {
    e.printStackTrace();
}
```

**Con HyfeUtils:**
```java
FileConfiguration config = hyfe.config().load("config.yml");
String message = config.getString("messages.welcome", "&aBienvenido");
hyfe.config().save(config, "config.yml");
```

---

### Scheduler con limpieza automática

**Sin HyfeUtils:**
```java
private final List<BukkitTask> tasks = new ArrayList<>();

// En onEnable:
tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
    // lógica
}, 0L, 20L));

tasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
    // lógica async
}, 0L, 100L));

// En onDisable - debes recordar cancelar cada tarea
for (BukkitTask task : tasks) {
    task.cancel();
}
```

**Con HyfeUtils:**
```java
hyfe.scheduler().runRepeating(0L, 20L, () -> { /* lógica */ });
hyfe.scheduler().runRepeatingAsync(0L, 100L, () -> { /* async */ });

// En onDisable:
hyfe.scheduler().cancelAll(); // cancela todo de golpe
```

---

### BossBar

**Sin HyfeUtils (NMS/Adventure manual en 1.8.8):**
```java
// Necesitas detectar versión, crear boss bar manualmente
// y manejar packets o usar API de Bukkit (solo 1.9+)
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

// Crear audiences (necesitas manage manualmente)
BukkitAudiences audiences = BukkitAudiences.create(plugin);

// Crear boss bar
BossBar bossBar = BossBar.bossBar(
    Component.text("§6§lBoss"),
    1.0f,
    BossBar.Color.PINK,
    BossBar.Overlay.PROGRESS,
    java.util.EnumSet.noneOf(BossBar.Flag.class)
);

// Mostrar
audiences.player(player).showBossBar(bossBar);

// Actualizar progreso
bossBar.progress(0.5f);

// Cambiar color (requiere recrear)
BossBar newBar = BossBar.bossBar(
    Component.text("§c§lFase 2"),
    0.75f,
    BossBar.Color.RED,
    BossBar.Overlay.PROGRESS,
    java.util.EnumSet.noneOf(BossBar.Flag.class)
);
audiences.player(player).hideBossBar(bossBar);
audiences.player(player).showBossBar(newBar);

// Ocultar
audiences.player(player).hideBossBar(newBar);
```

**Con HyfeUtils:**
```java
// Crear y mostrar en una línea
hyfe.bossBar().builder()
    .name("&6&lBoss")
    .color("RED")
    .progress(1.0f)
    .show(player);

// Actualizar progreso dinámicamente
BossBar bar = hyfe.bossBar().builder()
    .name("&6&l¡Ronda 3!")
    .color(BossBar.Color.GREEN)
    .progress(0.75f)
    .build();
hyfe.bossBar().show(player, bar);

bar.progress(0.5f);
bar.name(Component.text("§c§l¡Última fase!"));

// Ocultar
hyfe.bossBar().hide(player, bar);
```

---

## Mensajes

Todos los métodos aceptan `CommandSender`, por lo que funcionan con jugadores,
la consola y comandos remotos:

```java
hyfe.messages().send(player, "&aHola, &f" + player.getName());
hyfe.messages().send(sender, "&cNo tienes permiso.");
hyfe.messages().send(getServer().getConsoleSender(), "&eServidor iniciado");
```

### Mensajes centrados

Los mensajes se centran automáticamente en el chat (176 píxeles de ancho)
usando la tabla de font metrics de Minecraft:

```java
hyfe.chat().sendCentered(player, "&6&lMCLANS");
hyfe.chat().sendCentered(player, "&7Bienvenido al servidor");
hyfe.chat().sendCentered(sender, "&eRanking de clanes");
```

### Textos clickeables

Construye mensajes con acciones de clic y hover usando Adventure:

```java
import static com.hyfecraft.hyfeutils.text.ClickableTextService.clickable;

// Ejecutar comando al hacer clic
hyfe.clickableText().send(player,
    clickable("&a[Info] &7Haz clic para ver tu clan")
        .runCommand("/clan info")
        .hoverText("&eClic para información")
        .build()
);

// Sugerir comando
hyfe.clickableText().send(player,
    clickable("&b[Escribir] &7Haz clic para escribir")
        .suggestCommand("/clan ")
        .hoverText("&7Escribe un comando de clan")
        .build()
);

// Abrir enlace
hyfe.clickableText().send(player,
    clickable("&6[Web] &7Visita nuestra web")
        .openUrl("https://example.com")
        .hoverText("&eAbrir en el navegador")
        .build()
);

// Copiar al portapapeles
hyfe.clickableText().send(player,
    clickable("&e[IP] &7Clic para copiar la IP")
        .copyToClipboard("play.example.com")
        .hoverText("&7Clic para copiar")
        .build()
);
```

Para obtener solo el texto convertido:

```java
String legacy = hyfe.text().colorize("&bTexto &#FF8800con color");
String rgb = hyfe.text().colorize("&#12ABEFTexto", 735); // protocolo 1.16+
```

## Títulos y actionbar

Los tiempos se expresan en ticks: 20 ticks equivalen aproximadamente a un
segundo.

```java
hyfe.titles().send(player,
        "&#00AAFF¡Ronda completada!",
        "&fRecompensa: &e500 monedas",
        10, 60, 10);

hyfe.actionBar().send(player, "&7Vida: &c" + player.getHealth());
```

### Títulos animados

Efecto ola con dos colores que se alternan por carácter:

```java
// Ola simple: color base &6 (dorado), color de ola &f (blanco)
hyfe.animatedTitles().wave(player,
    "&6&l¡COMIENZA LA PARTIDA!",
    "&f&l",   // color de ola
    60,       // duración total en ticks (3 segundos)
    3         // delay entre frames (ticks)
);

// Ola con subtitle
hyfe.animatedTitles().waveWithSubtitle(player,
    "&6&l¡COMIENZA!",
    "&7Prepárate para luchar",
    "&f&l",
    60, 3
);

// Rainbow wave (arcoíris automático)
hyfe.animatedTitles().rainbowWave(player,
    "&l¡PARTIDA!",
    80, 3
);

// Rainbow wave con subtitle
hyfe.animatedTitles().rainbowWaveWithSubtitle(player,
    "&l¡PARTIDA!",
    "&7Modo supervivencia",
    80, 3
);

// Animación con frames personalizados
hyfe.animatedTitles().animate(player,
    List.of("&4Frame 1", "&6Frame 2", "&aFrame 3"),
    60  // ticks totales
);
```

El efecto ola funciona aplicando dos colores alternados por carácter y
desplazando el patrón cada frame para crear el efecto de movimiento.

## BossBar

Las BossBar funcionan en **todas las versiones** incluyendo 1.8.8. Usa el builder
fluent para configurar colores, estilos y progreso:

```java
BossBar bar = hyfe.bossBar().builder()
    .name("&6&l¡BOSS!")
    .color("RED")
    .overlay("NOTCHED_12")
    .progress(1.0f)
    .build();

hyfe.bossBar().show(player, bar);
```

### Colores disponibles

| Color     | Código       |
|-----------|--------------|
| `PINK`    | Por defecto  |
| `BLUE`    | Azul         |
| `GREEN`   | Verde        |
| `YELLOW`  | Amarillo     |
| `PURPLE`  | Morado       |
| `WHITE`   | Blanco       |
| `RED`     | Rojo         |

### Estilos de barra

| Estilo        | Descripción                        |
|---------------|------------------------------------|
| `PROGRESS`    | Barra sólida (por defecto)         |
| `NOTCHED_6`   | Dividida en 6 segmentos            |
| `NOTCHED_10`  | Dividida en 10 segmentos           |
| `NOTCHED_12`  | Dividida en 12 segmentos           |
| `NOTCHED_20`  | Dividida en 20 segmentos           |

### Flags opcionales

```java
BossBar bar = hyfe.bossBar().builder()
    .name("&c&l¡FASE FINAL!")
    .color("RED")
    .darkenScreen(true)      // Oscurece el cielo
    .playBossMusic(true)     // Reproduce música de boss
    .createWorldFog(true)    // Crea niebla
    .build();
```

### Actualización dinámica

El `BossBar` de Adventure permite modificar propiedades después de crearlo:

```java
BossBar bar = hyfe.bossBar().builder()
    .name("&6&lRonda 1")
    .color("GREEN")
    .progress(1.0f)
    .build();

hyfe.bossBar().show(player, bar);

// Actualizar progreso y nombre
bar.progress(0.5f);
bar.name(Component.text("§6§lRonda 2"));

// Ocultar
hyfe.bossBar().hide(player, bar);
```

### Soporte de versiones

| Versión del servidor | Versión del cliente | Método usado           |
|----------------------|---------------------|------------------------|
| 1.8.8                | 1.8.x               | Con ViaVersion: paquetes |
| 1.8.8                | 1.9+                | Paquetes ViaVersion    |
| 1.9+                 | Cualquiera          | Bukkit API o Adventure |

## Scheduler

Las tareas deben ejecutarse en el hilo principal cuando interactúan con Bukkit.
Usa las variantes asíncronas para operaciones bloqueantes como consultas de
base de datos o peticiones HTTP.

```java
hyfe.scheduler().runLater(20L, () ->
        hyfe.messages().send(player, "&aHa pasado un segundo"));

hyfe.scheduler().runRepeating(0L, 20L, () -> {
    if (player.isOnline()) {
        hyfe.actionBar().send(player, "&7Tiempo: " + System.currentTimeMillis());
    }
});

hyfe.scheduler().runAsync(() -> {
    // Operación que no toca la API Bukkit.
});
```

## Eventos

`listen` devuelve un registro que puede cancelarse manualmente. Bukkit también
cancelará los registros automáticamente al desactivar el plugin huésped.

```java
EventRegistration joinRegistration = hyfe.events().listen(
        PlayerJoinEvent.class,
        event -> hyfe.messages().send(event.getPlayer(), "&aBienvenido"));
```

Con prioridad y soporte para eventos cancelables:

```java
hyfe.events().listen(PlayerInteractEvent.class, EventPriority.HIGH, true, event -> {
    // Solo recibe eventos no cancelados.
});
```

## Configuración

El archivo se busca en la carpeta de datos del plugin huésped. Si no existe,
se copia desde sus propios recursos.

```java
FileConfiguration config = hyfe.config().load("config.yml");
String message = config.getString("messages.welcome", "&aBienvenido");
hyfe.messages().send(player, message);

config.set("settings.enabled", true);
hyfe.config().save(config, "config.yml");
```

## Comandos

El comando debe existir en el `plugin.yml` del plugin huésped. HyfeUtils solo
evita el código repetitivo de asignar el executor y el tab completer.

```java
hyfe.commands().bind("saludar", (sender, command, label, args) -> {
    hyfe.messages().send(sender, "&aHola desde HyfeUtils");
    return true;
}, (sender, command, alias, args) -> Arrays.asList("uno", "dos"));
```

## JitPack

El repositorio público está disponible en
`https://github.com/LeonardoRRC/HyfeUtils`.

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.LeonardoRRC</groupId>
    <artifactId>HyfeUtils</artifactId>
    <version>1.0.3</version>
</dependency>
```

También puede usarse una etiqueta concreta, por ejemplo `1.0.2`, para evitar
que una compilación futura cambie el comportamiento de un plugin existente.

## Maven y shading

La API necesita estar dentro del jar del plugin que la utiliza. Las dependencias
de Adventure deben sombrearse y relocalizarse para evitar conflictos con otros
plugins:

```xml
<dependency>
    <groupId>com.hyfecraft</groupId>
    <artifactId>hyfe-utils</artifactId>
    <version>1.0.2</version>
</dependency>
```

Relocaliza al menos `net.kyori` a un paquete propio del plugin huésped. Spigot
API y ViaVersion no deben sombrearse.

Ejemplo de configuración para el `pom.xml` del plugin huésped:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.3</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>net.kyori</pattern>
                        <shadedPattern>com.example.myplugin.libs.kyori</shadedPattern>
                    </relocation>
                </relocations>
                <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
        </execution>
    </executions>
</plugin>
```

La clase principal del plugin huésped debe llamar a `HyfeUtils.create(this)` en
`onEnable()` y a `close()` en `onDisable()`.
