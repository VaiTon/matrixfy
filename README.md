# Matrixfy

A simple bot to control your spotify account from matrix

## Build

1. Checkout the repository
2. Build via `./gradlew build`

## Deploy

> Before deplying you need to build the project

1. Copy one of the `build/distributions/*` files to your server, then unzip/untar the file.
2. Run the `bin/` script. The server will create a config.json file in the same directory and exit
3. Create a spotify app at the [spotify developer hub](https://developer.spotify.com/dashboard/applications).
4. Add
   the `spotifyClientId` and `spotifyClientSecret` to the `config.json` file
5. Run the `bin/` script again
6. The server will print a URL to the console, open it in your browser and login with the selected matrix account.
7. Copy the redirect url and paste it into the console
8. Enjoy! :)

## Interact

The bot still doesn't support joining rooms.

For now, you'll need to first log in with the same account of the bot on a matrix client and join the desired room.

Then, you can send commands to the bot via any account that is in the room.

| Command           | Description          |
|-------------------|----------------------|
| `!login`          | Prints the login url |
| `!search <query>` | Searches for a song  |