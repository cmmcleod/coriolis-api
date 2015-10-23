# Coriolis API

TODO - add generation description


## API Overview

See the wiki for an overview of the API and it's capabilities.

## Development

### Dependencies

```
git submodule update --init
```

### Build

```
mvn package
```

### Testing

```
mvn test
```

### Running

Run DynamoDB Database locally:

```
java -Djava.library.path=./dynamodb_local_2015-07-16_1.0/DynamoDBLocal_lib -jar dynamodb_local_2015-07-16_1.0/DynamoDBLocal.jar -dbPath .
```

```
java -jar target/api-[version].jar server dev.yml
```

Running in production should only need the following memory:

```
java -XX:+UseConcMarkSweepGC -Xmx512M -XX:MaxPermSize=64M -XX:PermSize=32M -jar target/api-[version].jar server dev.yml
```

See [pom.xml](http://link_to_something) for the current `version`.

### License

The code for api.coriolis.io is released under the MIT License.

Copyright (c) 2015 Coriolis.io, Colin McLeod

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software (Javascript, CSS, HTML, and SVG files only), and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.