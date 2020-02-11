gRPC官方demo使用明文传输数据，这在生产环境是不推荐的，第三方能够非常简单的监听、伪造或者发动中间人攻击。因此官方推荐在生产环境下，必须使用gRPC提供的安全机制。gRPC设计可以使用多种身份验证机制，可以使用TLS/SSL，或者基于Google令牌的身份验证，也支持开发者实现接口自己定义认证过程。

## gRPC 的安全机制
1.通道凭证：默认提供了基于 HTTP/2 的 TLS，对客户端和服务端交换的所有数据进行加密传输；除了传统的服务端认证之外，gRPC也支持双向认证。  
2.调用凭证：被附加在每次 RPC 调用上，通过 Credentials 将认证信息附加到消息头中，由服务端做授权认证；类似前后端调用的header_token。  
3.组合凭证：将一个频道凭证和一个调用凭证关联起来创建一个新的频道凭证，在这个频道上的每次调用会发送组合的调用凭证来作为授权数据。上两种方案的组合。  
4.Google 的 OAuth 2.0：gRPC 内置的谷歌的 OAuth 2.0 认证机制，通过 gRPC 访问 Google API 时，使用 Service Accounts 密钥作为凭证获取授权令牌。  

## SSL/TLS
SSL/TLS 协议是为了解决明文传输的安全风险：窃听风险、篡改风险、冒充风险。
相应的，SSL/TLS 协议采用了几个方式应对：  
1.加密：使用非对称加密和对称加密，主要是为解决窃听风险。  
2.校验：数字签名，主要是为解决篡改风险。  
3.安全证书：数字证书，主要为解决冒充风险。  

非对称加密和对称加密我们已经很熟悉了，下面简单介绍一下数字证书和数字签名是如何在我们的电脑当中工作的。  
数字证书是一个经证书授权中心(CA)数字签名的包含公开密钥拥有者信息以及公开密钥的文件。数字证书在SSL/TLS传输过程中扮演身份认证和密钥分发的功能。  
数字签名主要是为了解决证书被篡改的风险。方式是对证书原文进行HASH计算得到一个报文摘要，然后使用私钥进行加密，被连同数字证书一块发送给对方。对方收到数据之后，就可以通过你的公钥解密，得到hash值，同时使用相同算法计算证书HASH，比对两个的值，以此来判断数据是否经过了第三方篡改。至于为什么多了一个生成报文摘要的环节，是因为对原文进行对称加密往往比较耗时，通过对于摘要的加密仍然可以达到相同的作用。另外RSA加密要求数据长度不能超过密钥长度，比如RSA2048对应255字节，证书中可能包含更多的信息，所以在加密之前需要进行一下HASH。  

那么我作为一个服务提供者，如何制作我的证书并且能够让客户端信任呢？
下面是我结合自己的理解，整理的一个证书制作验证的简化流程，以https为例：
- 申请证书  
  1.服务提供者首先会生成一对密钥，即服务端的公钥和私钥，服务端保证私钥只有自己拥有。  
  2.服务端将自己要生成证书的信息（标准的信息包括组织信息、公用名称、城市等等）使用私钥加密，附上自己的公钥（CSR：证书签名请求文件），发送给证书授权中心CA。  
  3.CA通过得到的公钥解密，获取证书请求当中的信息。通过线下或者线上验证信息是否真实（组织是否存在、域名是否由申请者持有等等），如果通过验证。就会签发证书，证书包含以下信息：申请者公钥、申请者的组织/个人等信息、签发机构的信息、证书有效时间、序列号、摘要算法等信息的明文；同时附加上述信息签名（HASH后使用CA的私钥进行加密）后的值。将这个证书文件发给申请者。  
  上面的证书信息是一个简化的描述，在X509标准当中，包含很多的字段。申请证书的过程中，申请者和CA都是用自己的私钥进行加密，但并不公开私钥，要验证信息是否正确要用对应的公钥进行解密。
- 部署证书  
  1.服务提供者拿到证书后会部署到服务器上，例如nginx或者tomcat会有对应的配置(grpc-java 基于netty，也支持tls)，一般需要配置证书和自己的私钥。  
- 客户端验证  
  首先要明确的是，我们的操作系统或者浏览器在安装的时候拥有一个存储区，里面内置了所有合法的CA机构的证书，这些证书包含CA机构的所有信息以及CA的公钥。只有存储区内包含的公钥才会被浏览器或操作系统信任。当然这个证书列表用户是可以自由删除或添加证书的。  
  1.客户端发出请求，发送客户端SSL版本等信息，服务端返回证书文件。  
  2.客户端校验证书是否合法，首先读取证书的明文信息，使用里面记录的摘要算法对明文信息HASH，得到摘要值；读取明文信息记录的签发机构，向信任证书存储区查找CA的公钥（在实际情况下签发机构很可能是一个信任链，如果证书不被信任，中止请求），使用CA公钥对证书的签名进行解密，比对解密得到的摘要值和自己计算的是否一致，否则中止请求；验证服务器域名和证书的公用名称(新标准使用subjectAltName判断，支持指定多个域名和IP)是否一致、证书有效期等信息。  
  3.客户端发送自己可支持的对称加密方案给服务端，供其选择。服务端选择一个安全的加密方式，以明文方式返回给客户端。  
  4.客户端收到加密方式后，随机生成一个对称加密的密钥，用服务端公钥加密后，发给服务端（公钥加密数据只能由私钥解密，且私钥只有服务端有）。  
  5.服务端用自己的私钥解密，得到对称加密的密钥。  
  6.从此客户端和服务端使用上面沟通到的密钥加密数据。  
  
下面使用openssl生成证书。
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