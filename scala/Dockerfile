FROM ubuntu:22.04

RUN apt update && apt upgrade -y

RUN apt install curl nano -y

RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > cs && chmod +x cs && yes | ./cs setup

RUN grep -i export ~/.profile >> ~/.bashrc 

RUN echo 'PS1="SCALA | $PS1"' >> ~/.bashrc

WORKDIR /work

CMD bash