$jar = 'C:\Users\li\.m2\repository\org\springframework\security\spring-security-crypto\6.2.0\spring-security-crypto-6.2.0.jar'
$jcl = 'C:\Users\li\.m2\repository\org\springframework\spring-jcl\5.0.5.RELEASE\spring-jcl-5.0.5.RELEASE.jar'
Set-Location 'e:\javaeeworkspace\kb-deploy\gen'
javac -cp $jar SeedHashGen.java
java -cp ".;$jar;$jcl" SeedHashGen 123456
