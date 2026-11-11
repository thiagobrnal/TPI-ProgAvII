package com.gestionamateur.tpi.progav.seguridad;

public class TestVectorsBCrypt {
    public static void main(String[] args) {
        // Vectores canónicos oficiales de Openwall (crypt_blowfish) y Bouncy Castle (OpenBSDBCryptTest)
        String[][] openwall_vectors = {
            {"U*U", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.E5YPO9kmyuRGyh0XouQYb4YMJKvyOeW"},
            {"U*U*", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.VGOzA784oUp/Z0DY336zx7pLYAy0lwK"},
            {"U*U*U", "$2a$05$XXXXXXXXXXXXXXXXXXXXXO", 
             "$2a$05$XXXXXXXXXXXXXXXXXXXXXOAcXxm9kjPGEMsLznoKqmqw7tc8WCx4a"},
            {"", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.7uG0VCzI2bS7j6ymqJi9CdcdxiRTWNy"},
            {"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789chars after 72 are ignored",
             "$2a$05$abcdefghijklmnopqrstuu", 
             "$2a$05$abcdefghijklmnopqrstuu5s2v8.iXieOjg/.AySBTTZIIVFJeBui"},
            {"password", "$2a$05$bvIG6Nmid91Mu9RcmmWZfO", 
             "$2a$05$bvIG6Nmid91Mu9RcmmWZfO5HJIMCT8riNW0hEp8f6/FuA2/mHZFpe"},
            {"", "$2a$06$DCq7YPn5Rq63x1Lad4cll.", 
             "$2a$06$DCq7YPn5Rq63x1Lad4cll.TV4S6ytwfsfvkgY8jIucDrjc8deX1s."},
            {"", "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.", 
             "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.Tl.ZHfXLhvt/SgVyWhQqgqcZ7ZuUtye"},
            {"", "$2a$10$k1wbIrmNyFAPwPVPSVa/ze", 
             "$2a$10$k1wbIrmNyFAPwPVPSVa/zecw2BCEnBwVS2GbrmgzxFUOqW9dk4TCW"},
            {"", "$2a$12$k42ZFHFWqBp3vWli.nIn8u", 
             "$2a$12$k42ZFHFWqBp3vWli.nIn8uYyIkbvYRvodzbfbK18SSsY.CsIQPlxO"}
        };

        boolean allOk = true;
        for (int i = 0; i < openwall_vectors.length; i++) {
            String plain = openwall_vectors[i][0];
            String salt = openwall_vectors[i][1];
            String expected = openwall_vectors[i][2];

            long t0 = System.currentTimeMillis();
            String computed = BCrypt.hashpw(plain, salt);
            long dt = System.currentTimeMillis() - t0;

            boolean match = computed.equals(expected);
            boolean checkMatch = BCrypt.checkpw(plain, expected);

            System.out.println("Vector " + i + " ('" + (plain.length() > 20 ? plain.substring(0, 20) + "..." : plain) 
                + "', " + salt.substring(0, 7) + "): " 
                + (match ? "PASSED" : "FAILED") 
                + " (checkpw: " + checkMatch + ", time: " + dt + "ms)");
            if (!match) {
                System.out.println("  Expected: " + expected);
                System.out.println("  Computed: " + computed);
                allOk = false;
            }
        }

        if (allOk) {
            System.out.println("\n>>> ALL 10 OFFICIAL OPENWALL / BOUNCY CASTLE TEST VECTORS PASSED 100%! <<<");
        } else {
            System.err.println("\nSOME VECTORS FAILED!");
            System.exit(1);
        }
    }
}
