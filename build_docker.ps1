docker.exe build -t muzei-bingimageoftheday .

# for Docker Toolbox (not sure if Docker would need this)
$path = $PSScriptRoot
$path = $path -replace ':', ''
$path = $path -replace '\\', '/'
$path = $path -replace 'c/Users', '/c/Users'

docker.exe run --rm -it -v ${path}:/home/app/ -w /home/app muzei-bingimageoftheday /bin/bash -c "bash_r .ci/build.sh"
