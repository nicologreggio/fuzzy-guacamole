FROM rust:1.63

RUN apt update && apt upgrade -y

RUN apt install curl nano cmake -y

RUN echo 'PS1="RUST | $PS1"' >> ~/.bashrc

COPY . /work

WORKDIR /work/rust-mqtt

RUN cargo build --bin rust-mqtt

CMD bash