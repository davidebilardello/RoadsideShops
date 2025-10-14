# Roadside Shops

![Logo](https://raw.githubusercontent.com/EscanorTargaryen/RoadsideShops/dev/imgs/Logo2.png)


Dive deeper into the seller's side with Roadside shops! With newspaper sponsor system.
<hr>

**Modrinth Page:** <https://modrinth.com/plugin/roadside-shops/>  
**Spigot Page:** <https://www.spigotmc.org/resources/107148/>  
**Discord Server:** <https://discord.gg/KRS7dSU6tR> 

## ReadyToTest zip
Download in one zip everything you need to test RoadsideShops going in one click!

The zip will contain:
- Vault v1.7.3
- [SimpleEconomy](https://github.com/EscanorTargaryen/SimpleEconomy) v1.0.2
- RoadsideShops v1.1.19

Click [here](https://modrinth.com/plugin/roadside-shops/version/1.1.19) for the download!

> Note! **SimpleEconomy** was released in order to be able to test RoadsideShops quickly. It is recommended to use another economy plugin of your choice that is compatible with Vault.

## Maven for developers

```xml
<repository>
    <id>escanortargaryen</id>
    <url>https://nexus.frengor.com/repository/escanortargaryen-public/</url>
</repository>
```   
```xml
<dependency>
    <groupId>it.escanortargaryen</groupId>
    <artifactId>roadsideshops</artifactId>
    <version>1.1.19</version>
    <scope>provided</scope>
</dependency>
```

## License

This project is licensed under the [GNU Lesser General Public License v3.0 or later](https://www.gnu.org/licenses/lgpl-3.0.txt).

## Credits

RoadsideShops has been made by [EscanorTargaryen](https://github.com/EscanorTargaryen).  
The pluign uses the following libraries:
* [CommandAPI](https://github.com/JorelAli/CommandAPI) (released under MIT license) to add commands to the plugin
* [AnvilGUI](https://github.com/WesJD/AnvilGUI) (released under MIT license) to set the price of the items
* [Vault](https://github.com/MilkBowl/Vault) (released under LGPL license) to transfer money in the game
