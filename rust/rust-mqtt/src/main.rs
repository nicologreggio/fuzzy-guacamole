use std::ffi::OsString;
use std::fmt::format;
use std::fs::File;
use std::io::{self, BufRead, BufReader, Lines};
use std::path::Path;
use std::{env, process, thread, time::Duration};
use regex::Regex;

use mqtt::Client;

extern crate paho_mqtt as mqtt;

const DFLT_BROKER: &str = "tcp://fuzzy-guacamole-mqtt-1:1883";
const BASE_CLIENT_ID: &str = "unit_";
const RESULT_TOPIC: &str = "results";
const NEW_CLIENT_TOPIC: &str = "new_client";
const DFLT_QOS: &[i32] = &[0, 1];

fn main() {
    let unit_id=env::var_os("UNIT").unwrap_or_else(|| "x".into()).to_str().unwrap().to_owned();
    let client_id=[BASE_CLIENT_ID, &unit_id].join("");
    
    // read_file_line_by_line("/library/Bible.txt");

    let host=env::var_os("BROKER_ADDRESS")
        .unwrap_or_else(|| DFLT_BROKER.into()).to_str().unwrap().to_owned();

    println!("we got {:?} and {:?}", client_id, host);

    // Define the set of options for the create.
    // Use an ID for a persistent session.
    let create_opts = mqtt::CreateOptionsBuilder::new()
        .server_uri(host)
        .client_id(BASE_CLIENT_ID.to_string())
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
    
    join_server(&cli, &client_id);
    
    let mut filename: &str;
    let mut regex: &str;
    let mut length: &str;
    let mut offset: &str;
    for msg in rx.iter() {
        if let Some(msg) = msg {
            let payload=msg.payload_str();
            let mut l: Vec<&str>=payload.split(":::").collect();  //.for_each(|m| println!("\n{}", m))
            (offset, length, regex, filename)=(l.pop().unwrap(), l.pop().unwrap(), l.pop().unwrap(), l.pop().unwrap());
            println!("file{}, reg {}, len {}, off {}", filename, regex, length, offset);
            let results=find_matches(filename, regex, offset.parse::<i32>().unwrap(), length.parse::<i32>().unwrap());
            let res_msg=format!("{} found: \n{}", &client_id, &results);
            let msg = mqtt::Message::new(RESULT_TOPIC, res_msg.as_bytes(), 1);
            println!("\nsent {}\n", &msg);
            let tok = cli.publish(msg);
            if let Err(e) = tok {
                println!("Error sending message: {:?}", e);
            }
            // send_back(msg, &cli)

        } else if !cli.is_connected() {
            if try_reconnect(&cli) {
                println!("Resubscribe topics...");
                // subscribe_topics(&cli);
            } else {
                break;
            }
        }
    }
    
}

fn join_server(cli: &Client, id: &String) -> () {
    let msg = mqtt::Message::new(NEW_CLIENT_TOPIC, id.as_bytes(), 1);
    let tok = cli.publish(msg);
    if let Err(e) = cli.subscribe(id, 1){
        println!("Error while subscribing to {} topic message: {:?}", &id, e);
    } else{
        println!("Successfully subscribed to {} topic, waiting jobs to do...", &id);
    }

    if let Err(e) = tok {
        println!("Error sending message: {:?}", e);
    }
}

fn find_matches(filepath: &str, regex: &str, offset: i32, length: i32) -> String{
    // Open the file
    let file = File::open(filepath).unwrap();
    let reader = BufReader::new(file);

    // loads assinged chunk
    let chunk=reader.lines()
        .skip(offset as usize)
        .take(length as usize)
        .map(|e| e.unwrap())
        .reduce(|a,b| a+&b).unwrap();


    // initialize regex and look for matches
    // let re=Regex::new(r"\d{2}\w\d{3}").unwrap();
    let re=Regex::new(format!(r"{}", regex).as_str()).unwrap();
    let res=re.find_iter(&chunk);
    
    // build results
    // let all_results=res.enumerate()
    //     .fold(format!("Matches from {}: ", client_id), |acc, (idx, val)| format!("{}\n{}) {}", acc, idx, val.as_str()));
    let all_results=res.enumerate()
        .fold(String::from(""), |acc, (idx, val)| format!("{}\n{}) {}", acc, idx, val.as_str()));
    
    println!("With {}, Found: {}", re, all_results);
    return all_results;
}

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

fn read_file_line_by_line(filepath: &str) -> Result<(), Box<dyn std::error::Error>> {
    let file = File::open(filepath)?;
    let reader = BufReader::new(file);

    // for line in reader.lines() {
    //     println!("{}", line?);
    // }

    // println!("{}", reader.lines().count());
    let r=reader.lines()
        .skip(1)
        .take(3)
        .map(|e| e.unwrap())
        .reduce(|a,b| a+&b);
    
    println!("{}", r.unwrap());

    Ok(())
}
