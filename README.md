## grpc-java 证书通信 demo 验证

这个Demo是强制服务器、客户端认证的示例

生成CA证书、服务器和客户端证书(代码当中使用的CommonName=localhost，也可以使用X509主题替代名称(subjectAltName)同时指定多个域名或IP)，证书也可以在代码当中生成：
```bash
openssl genrsa -des3 -out ca.key 4096
openssl req -new -x509 -days 365 -key ca.key -out ca.crt -subj "/CN=localhost"
openssl genrsa -des3 -out server.key 4096
openssl req -new -key server.key -out server.csr -subj "/CN=localhost"
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt 
openssl rsa -in server.key -out server.key
openssl genrsa -des3 -out client.key 4096
openssl req -new -key client.key -out client.csr -subj "/CN=localhost"
openssl x509 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out client.crt
openssl rsa -in client.key -out client.key
openssl pkcs8 -topk8 -nocrypt -in client.key -out client.pem
openssl pkcs8 -topk8 -nocrypt -in server.key -out server.pem
```
因为CA证书是自签名的证书，默认不被系统信任，要在服务启动时设置到信任列表当中：
Server端（MessageServer.java）：
```
SslContext sslContext = GrpcSslContexts.forServer(new File(keyCertChainFilePath), new File(keyFilePath)) // 服务端证书、私钥
                .trustManager(new File(trustCertPath))
                // ...
                .build(); // 传入CA证书到信任列表
```
Client端（MessageClient.java）:
```
SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(new File(trustCertPath)) //传入CA证书到信任列表
                .keyManager(new File(classPath + keyCertChainFilePath), new File(classPath + keyFilePath)) //客户端证书、私钥
                // ...
                .sslProvider(SslProvider.OPENSSL).build();
```
这样GRPC将会通过证书认证，加密连接。

[Grpc-java tls认证官方文档](https://github.com/grpc/grpc-java/blob/master/SECURITY.md)