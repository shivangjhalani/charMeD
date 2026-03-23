{ pkgs, ... }:

{
  env.GREET = "charMeD — Terminal Markdown Editor";

  packages = [
  ];

  languages.java.enable = true;
  languages.java.jdk.package = pkgs.temurin-bin-21;
  languages.java.gradle.enable = true;

  languages.go.enable = true;

  scripts.build-all.exec = ''
    echo "Building Backend..."
    cd backend && ./gradlew shadowJar
    echo "Building Frontend..."
    cd ../frontend && go build -o charmed .
  '';

  enterShell = ''
    echo $GREET
    java -version
    go version
  '';
}
