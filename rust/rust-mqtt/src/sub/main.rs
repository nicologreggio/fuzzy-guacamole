use std::{env, process, thread, time::Duration};

use mqtt::Client;

extern crate paho_mqtt as mqtt;

const DFLT_BROKER: &str = "tcp://fuzzy-guacamole-mqtt-1:1883";
const DFLT_CLIENT: &str = "rust_publish";
const DFLT_TOPICS: &[&str] = &["pippo", "pluto"];
const DFLT_QOS: &[i32] = &[0, 1];

fn main() {
    println!("Hello, from subscriber");

    let host = env::args()
        .nth(1)
        .unwrap_or_else(|| DFLT_BROKER.to_string());

    // Define the set of options for the create.
    // Use an ID for a persistent session.
    let create_opts = mqtt::CreateOptionsBuilder::new()
        .server_uri(host)
        .client_id(DFLT_CLIENT.to_string())
        .finalize();

    // Create a client.
    let cli = mqtt::Client::new(create_opts).unwrap_or_else(|err| {
        println!("Error creating the client: {:?}", err);
        process::exit(1);
    });

    // Define the set of options for the connection.
    let conn_opts = mqtt::ConnectOptionsBuilder::new()
        .keep_alive_interval(Duration::from_secs(20))
        .clean_session(true)
        .finalize();

    // Connect and wait for it to complete or fail.
    if let Err(e) = cli.connect(conn_opts) {
        println!("Unable to connect:\n\t{:?}", e);
        process::exit(1);
    }

    // Initialize the consumer before connecting.
    let rx = cli.start_consuming();
    // Subscribe topics.
    subscribe_topics(&cli);

    println!("Processing requests...");
    for msg in rx.iter() {
        if let Some(msg) = msg {
            println!("{}", msg);
            send_back(msg, &cli)
        } else if !cli.is_connected() {
            if try_reconnect(&cli) {
                println!("Resubscribe topics...");
                subscribe_topics(&cli);
            } else {
                break;
            }
        }
    }
}

fn send_back(msg: mqtt::Message, cli: &Client) {
    let msg = paho_mqtt::Message::new(DFLT_TOPICS[0], msg.payload(), 1);
    let tok = cli.publish(msg);

    if let Err(e) = tok {
        println!("Error sending message: {:?}", e);
    }
}

fn subscribe_topics(cli: &mqtt::Client) {
    if let Err(e) = cli.subscribe_many(DFLT_TOPICS, DFLT_QOS) {
        println!("Error subscribes topics: {:?}", e);
        process::exit(1);
    }
}

// Reconnect to the broker when connection is lost.
fn try_reconnect(cli: &mqtt::Client) -> bool {
    println!("Connection lost. Waiting to retry connection");
    for _ in 0..12 {
        thread::sleep(Duration::from_millis(5000));
        if cli.reconnect().is_ok() {
            println!("Successfully reconnected");
            return true;
        }
    }
    println!("Unable to reconnect after several attempts.");
    false
}
