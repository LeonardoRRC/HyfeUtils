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

```java
hyfe.messages().send(player, "&7Bienvenido &#12ABEF" + player.getName());
hyfe.actionBar().send(player, "&#00FFAA¡Objetivo completado!");
hyfe.titles().send(player, "&#FFAA00Victoria", "&fHas ganado", 10, 40, 10);
```

Los formatos `&a` y `&#RRGGBB` son compatibles. En clientes 1.16 o superiores
se utiliza RGB; en clientes antiguos se selecciona el color legacy más cercano.

## Mensajes

Todos los métodos aceptan `CommandSender`, por lo que funcionan con jugadores,
la consola y comandos remotos:

```java
hyfe.messages().send(player, "&aHola, &f" + player.getName());
hyfe.messages().send(sender, "&cNo tienes permiso.");
hyfe.messages().send(getServer().getConsoleSender(), "&eServidor iniciado");
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

Después de publicar el repositorio en GitHub, reemplaza `TU_USUARIO` por el
usuario u organización propietarios del repositorio:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.TU_USUARIO</groupId>
    <artifactId>HyfeUtils</artifactId>
    <version>1.0.0</version>
</dependency>
```

También puede usarse una etiqueta concreta, por ejemplo `1.0.0`, para evitar
que una compilación futura cambie el comportamiento de un plugin existente.

## Maven y shading

La API necesita estar dentro del jar del plugin que la utiliza. Las dependencias
de Adventure deben sombrearse y relocalizarse para evitar conflictos con otros
plugins:

```xml
<dependency>
    <groupId>com.hyfecraft</groupId>
    <artifactId>hyfe-utils</artifactId>
    <version>1.0.0</version>
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
