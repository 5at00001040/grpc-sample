gRPC Scala sample
====

## Configure TLS

download certs

```
mkdir -p /tmp/any/path/certs
cd /tmp/any/path/certs
curl -L -O https://github.com/grpc/grpc-go/raw/master/testdata/ca.pem
curl -L -O https://github.com/grpc/grpc-go/raw/master/testdata/server1.pem
curl -L -O https://github.com/grpc/grpc-go/raw/master/testdata/server1.key
```

add hosts

```
sudo sh -c "echo '127.0.0.1       grpc.test.google.fr' >> /etc/hosts"
sudo killall -HUP mDNSResponder
```

