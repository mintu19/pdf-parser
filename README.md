# pdf-parser
Custom Pdf Parser based on PdfBox


## Instructions

1) For building run
    `gradle clean build`



2) If dependencies gives error, clear cache
    `rm -rf ~/.gradle/caches/`



3) Final build is generated in build/distributions

    Jar is generated in build/libs/gmr-parser-<version>.jar



4) gradle install to create build/install with executables



## Examples

1) ./gmr-parser init -mw 300 -mh 300 -ar 0.2 -ard -i ~/example/

2) ./gmr-parser clean ~/example/