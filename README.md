# Studies project - Encrypted p2p communicator

## Collaboration:
- The project was created in cooperation with [**@Majkelevsky**](https://github.com/Majkelevsky/)

## About:
- Simple communicator based on TCP sockets.
- Multiple user accounts and automatically generated public and private RSA keys (stored locally in encrypted form).
- Fully encrypted message and file exchange between clients.
- AES, 3DES and DES transformations in ECB and CBC modes.
- Functional user GUI.

## Used technologies:
- TCP sockets
- Java Crypto
- JavaFX

## Communication scheme:
- After successful tcp connection, both clients establish communication by exchanging their usernames and public keys 
(using simple challenge / response mechanism).
- For each encryption settings alteration new session is established by exchanging session information and symmetric
key encrypted using RSA.
- One communication can hold many sessions.
- Once session is established, all data exchanged between clients is encrypted.

## Presentation:
- Sign in / Log in screen:

![login_screen](images/login.png)

- Main communicator window after successful login:

![main_window_ready](images/ready.png)

- One client (or both) start to listen for connections on desired port:

![main_window_listening](images/listening.png)

- Other client connects to a given ip and port, if successful connection will be established:

![main_window_connected](images/connected.png)

- From now on both clients can exchange chat messages:

![main_window_messages](images/mess.png)

- As well as send and receive files:

![main_window_sending](images/sending.png)
![main_window_receiving](images/receiving.png)
![main_window_complete](images/done.png)

- When finished clients can disconnect from chat or simply close the application:

![main_window_exit](images/exit.png)