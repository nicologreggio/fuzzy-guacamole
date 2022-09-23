#/bin/bash

files=(`ls library`)

tot=$((${#files[@]}-1))
for i in `seq 0 $tot`
do
    echo "$i) ${files[$i]}"
done

printf "\nChoose your file (enter number): "
read file

if [[ $file -lt 0 ]] || [[ $file -gt $tot ]] || [[ ! -f "library/${files[$i]}" ]]
then
    echo "Bad choice. Quit."
    exit 1
fi

printf "\nInput your regex: "
read regex

echo UNIT_NUMBER=2 > .env
echo FILENAME="/library/${files[$file]}" >> .env
echo REGEX=$regex >> .env

docker compose -f docker-compose-deploy.yml up #--build
#echo docker attach fuzzy-guacamole-scala-1