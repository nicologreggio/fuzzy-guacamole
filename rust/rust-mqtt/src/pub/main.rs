use std::env;
use std::process;
use std::time::Duration;

const DFLT_BROKER: &str = "tcp://fuzzy-guacamole-mqtt-1:1883";
const DFLT_CLIENT: &str = "rust_publish";
const DFLT_TOPICS: &[&str] = &["pippo", "pluto"];

fn main() {
    println!("Hello, from publisher");

    let host = env::args()
        .nth(1)
        .unwrap_or_else(|| DFLT_BROKER.to_string());

    // Define the set of options for the create.
    // Use an ID for a persistent session.
    let create_opts = paho_mqtt::CreateOptionsBuilder::new()
        .server_uri(host)
        .client_id(DFLT_CLIENT.to_string())
        .finalize();

    // Create a client.
    let cli = paho_mqtt::Client::new(create_opts).unwrap_or_else(|err| {
        println!("Error creating the client: {:?}", err);
        process::exit(1);
    });

    // Define the set of options for the connection.
    let conn_opts = paho_mqtt::ConnectOptionsBuilder::new()
        .keep_alive_interval(Duration::from_secs(20))
        .clean_session(true)
        .finalize();

    // Connect and wait for it to complete or fail.
    if let Err(e) = cli.connect(conn_opts) {
        println!("Unable to connect:\n\t{:?}", e);
        process::exit(1);
    }

    for num in 0..6 {
        let content = "Hello world! ".to_string() + &num.to_string();
        let mut msg = paho_mqtt::Message::new(DFLT_TOPICS[0], content.clone(), 1);
        if num % 2 == 0 {
            println!("Publishing messages on the {:?} topic", DFLT_TOPICS[1]);
            msg = paho_mqtt::Message::new(DFLT_TOPICS[1], content.clone(), 1);
        } else {
            println!("Publishing messages on the {:?} topic", DFLT_TOPICS[0]);
        }
        let tok = cli.publish(msg);

        if let Err(e) = tok {
            println!("Error sending message: {:?}", e);
            break;
        }
    }
}
