# Matrixfy

A simple bot to control your spotify account from matrix

## Build

1. Checkout the repository
2. Build via `./gradlew build`

## Deploy

> Before deplying you need to build the project

1. Create a matrix account for the bot on your favourite homeserver.
2. Copy one of the `build/distributions/*` files to your server, then unzip/untar the file.
3. Run the `bin/` script. The software will create a config.json file in the same directory and exit
4. Create a spotify app via the [spotify developer hub](https://developer.spotify.com/dashboard/applications) and get the client id and secret.
5. Add `spotifyClientId` and `spotifyClientSecret` to the `config.json` file
6. Run the `bin/` script again
7. The server will print a URL to the console, open it in your browser and login with the matrix account you first created for the bot.
8. Copy the redirect url and paste it into the console
9. Enjoy! :)

## Interact

The bot still doesn't support joining rooms.

For now, you'll need to first log in with the same account of the bot on a matrix client and join the desired room.

Then, you can send commands to the bot via any account that is in the room.

| Command           | Description          |
|-------------------|----------------------|
| `!login`          | Prints the login url |
| `!search <query>` | Searches for a song  |
