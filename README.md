<img src="assets/icon.svg" width="64" height="64" alt="">

# AutoLogin

Client-side Fabric mod that automatically logs you in on servers that ask for
it.

## Branches

One branch per Minecraft version line; CI builds every supported release from
each branch as a matrix job on [Blacksmith](https://www.blacksmith.sh)
runners.

| Branch    | Minecraft line     | Java | Build                             |
|-----------|--------------------|------|-----------------------------------|
| `main`    | 26.x and above     | 25   | Matrix: 26.1 – 26.3              |
| `1.21.11` | 1.21.11 (legacy)   | 21   | Single version                    |

## Building

The whole matrix builds from a single source tree through `-P` overrides:

```sh
./gradlew build -Pminecraft_version=26.3 -Pfabric_api_version=0.165.0+26.3 -Pminecraft_dependency="~26.3"
```

## License

[CC0-1.0](LICENSE)
