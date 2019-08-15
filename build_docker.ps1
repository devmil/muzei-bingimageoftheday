docker.exe build -t muzei-bingimageoftheday .

docker.exe run --rm -it -v ${PSScriptRoot}:/home/app/ -w /home/app muzei-bingimageoftheday /bin/bash -c "bash_r .ci/build.sh"
