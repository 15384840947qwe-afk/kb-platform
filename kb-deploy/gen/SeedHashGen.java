import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SeedHashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String pwd = args.length > 0 ? args[0] : "123456";
        System.out.println(encoder.encode(pwd));
    }
}
