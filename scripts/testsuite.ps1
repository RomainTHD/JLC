# It is assumed that the current working directory is the root, NOT the scripts subdirectory
$filename = $args[0]
wsl.exe sh -c "
export CLASSPATH=.:./lib/JLex.jar:./lib/cup.jar;
make all || exit;
mv submission.tar.gz $filename;
cd ./tester || exit;
export CLASSPATH=.:./lib/JLex.jar:./lib/cup.jar:../lib/JLex.jar:../lib/cup.jar;
python3 ./testing.py ../$filename --llvm -x objects1 arrays1;
"
Write-Output "Done."
