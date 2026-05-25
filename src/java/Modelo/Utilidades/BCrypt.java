// BCrypt.java - Damien Miller's Java implementation of OpenBSD's Blowfish-based password hashing
package Modelo.Utilidades;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

public class BCrypt {
    // Blowfish constants
    private static final int BLOWFISH_NUM_ROUNDS = 16;
    private static final int P_sz = BLOWFISH_NUM_ROUNDS + 2;

    private static final int[] P_init = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344, 0xa4093822, 0x299f31d0,
        0x082efa98, 0xec4e6c89, 0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917, 0x9216d5d9, 0x8979fb1b
    };

    private static final int[] S_init = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267e96,
        0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
        0x636920d8, 0x71574e69, 0xa458fea3, 0x99d938a5, 0x0cf883e2, 0x7a31b27d,
        0x12d8b060, 0x7f579feb, 0x1a82517b, 0xa74dec12, 0x3559348a, 0x140809b4,
        0x52e008b2, 0xe1ba949e, 0x3b1c46ae, 0xd3531ecb, 0x63103f83, 0x03c5149e,
        0x9f5c4075, 0x91d9e675, 0x64365922, 0x5a18a996, 0x5f58be92, 0x30132b85,
        0x2fd13c6e, 0x95973d42, 0x8bfd616c, 0x23a3cb8e, 0xc4c5e3f2, 0x060ca690,
        0xb2ff5f81, 0x21c81fb3, 0xed0cf8b6, 0xb4e1f7c9, 0x3a48e718, 0xd88a0889,
        0x323a7482, 0xe247fe01, 0xc15e2197, 0x0edc3848, 0x4f17fb42, 0x141525a7,
        0x45a9ca48, 0x1f5f3ef7, 0x85be199f, 0xdd1e0be6, 0xc2ee22f5, 0xb3f1a238,
        0xc46950f2, 0xd653fbda, 0xae793e22, 0xf1ffd00d, 0xed017a52, 0x59ec649c,
        0x69600e12, 0x3c094404, 0x3c2ed4cd, 0xd3ab2fe1, 0xc610c14c, 0xb977759d,
        0x0c648ef3, 0x498a4441, 0x4a806950, 0xdcd56885, 0x76136d42, 0x86705786,
        0xd1552a87, 0xc98b0f20, 0xb7308253, 0x12b59114, 0x48b61c16, 0xd1eb02fb,
        0xdd2d50fb, 0xe6e133e6, 0xd47568ed, 0x4db3cdcf, 0x1993427f, 0x6e8e2fa5,
        0x83e2003c, 0xc555c4fc, 0xdf05e267, 0xa1d5d109, 0x7ed09744, 0x63351d1e,
        0xe8a956d7, 0xe8e6db9d, 0xcbe3407e, 0x2280d908, 0xa6a75f85, 0x61ee3610,
        0x3235b31f, 0xe726190f, 0xc864a66e, 0xe00085a6, 0x6fa0d1df, 0xaf7e0340,
        0x6c6e1ff6, 0x82f25dbd, 0xc27a4d95, 0xb1b69680, 0x2274b7cd, 0xa2c39a79,
        0x9f5a7791, 0xe2a8ab27, 0x464a4d46, 0x69fcf74b, 0x70d8a57c, 0xd1d42878,
        0xe6f03d58, 0x6b093356, 0x2eecf130, 0xe116a4e3, 0x4a8e3d03, 0x8898144b,
        0xeb9143df, 0xd70743b1, 0x6a02b1f8, 0x446e100f, 0xc383c2a3, 0xe252bd8e,
        0xdf727edb, 0xc619ee65, 0xdb3c2859, 0xa8710ee9, 0x05f8846c, 0x9be041c2,
        0x454b15d0, 0xc3c5f6e9, 0x1d374f66, 0xe3e336d3, 0xc2df978d, 0xd888897a,
        0xf2abf27e, 0x3d03caee, 0x6a7d52f6, 0x673dbe8a, 0xa7c223c9, 0xc3191cde,
        0x79403bd9, 0xc75a133f, 0x93307521, 0x1df391d1, 0xba9b6264, 0xf6be6041,
        0x68cd171d, 0x19a0a0cb, 0x1fa12ad9, 0x6a2c206f, 0x7bd8a329, 0xb2f43db1,
        0x83f069bb, 0x8402ee32, 0xdfe861a1, 0xfad6ae6b, 0x2863eb3c, 0xe0a539fe,
        0x9b33a00f, 0xeb4b1ca7, 0x7d656041, 0xdd128e08, 0x00c73229, 0x16b0df22,
        0x19f563d7, 0xae168a3a, 0x40a8309a, 0x3dcf8dbb, 0xc3c94892, 0xdff9e472,
        0x5fb8a071, 0x1536b35b, 0xe4f7574b, 0xb0e872c6, 0xd8fd0a37, 0xe5819ad3,
        0x45db474c, 0xb2838bd2, 0xa3fbde88, 0xa5fb7b54, 0xa8f2df05, 0x3aeb0fbe,
        0xe8a90103, 0xcf0f1ca0, 0x2077e908, 0xfae1136b, 0xf02fa192, 0xbc332f1a,
        0x3235b2e9, 0x13c7bc37, 0x256e2996, 0xad5de7d8, 0xd6b7bf48, 0xe733732c,
        0xd1d4f3b5, 0x0b8a3b3a, 0x61858c14, 0x37850a55, 0x82db0e27, 0x0bc6c075,
        0x93309a63, 0xc0d866a4, 0x63311ef6, 0x936de3e8, 0x0446fae8, 0x10f7fc88,
        0xb029b35b, 0x8f7df43e, 0x9b35d7eb, 0xad9de3a5, 0x6b1d44a2, 0xc555c8fe,
        0xbf309f42, 0xc4eeaf83, 0x3b1c676d, 0x7623dc20, 0xe005a769, 0xe00cf5eb,
        0x805d7620, 0xe08fcdc4, 0x750ff8e5, 0x485af237, 0x1a719c83, 0x51c51886,
        0x4dfc418c, 0xf96b6b71, 0x969dc726, 0x39aef6bc, 0xdf05e0a6, 0x50ba9866,
        0x6a090fe6, 0xca62bb77, 0x39075775, 0x1fbe5343, 0x614f8263, 0x3b664dcf,
        0xbeecbe1f, 0x407bb033, 0x043deedc, 0xfade1d03, 0x1e3bd714, 0xa1d5d36e,
        0xa7d825c0, 0x1ef9798e, 0xbb3273e0, 0x514868e8, 0x76211756, 0xde35fe9f,
        0x19dfa5db, 0x7940250d, 0xc1d5f3bd, 0xbb5c0f44, 0x2280ce56, 0xb734d0b1,
        0xcbe2dc99, 0xbe2512f7, 0xcb99a6cf, 0x66cc9921, 0x9ab8184f, 0xa3fbca25,
        0x20556214, 0x8a928929, 0xe62e49c7, 0x2863eb4f, 0xbe7dca19, 0x356e9c97,
        0x272d5df9, 0x272b5354, 0xe11394a6, 0xe2a8b3be, 0xbbffc15b, 0x2e06ba5d,
        0x7cdc8c36, 0x13d806a0, 0x6335198e, 0x23a3cbbf, 0x31086207, 0x70d843ff,
        0xd1d109f6, 0xc1a12a52, 0xdf05e0fc, 0x07f1ff0d, 0x8a7f9b88, 0xe2f254ad,
        0xbc1ec6de, 0xe26210f0, 0x546e507b, 0x2ffd7220, 0x70d8a576, 0xbeecbd1f,
        0xc864a938, 0x9330a84e, 0x4f17fb03, 0x0446fad8, 0x6fa0d1df, 0x2ffd71ee,
        0x70d8e87d, 0xb2838933
    };

    private static final int[] S_init_0 = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267e96,
        0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
        0x636920d8, 0x71574e69, 0xa458fea3, 0x99d938a5, 0x0cf883e2, 0x7a31b27d,
        0x12d8b060, 0x7f579feb, 0x1a82517b, 0xa74dec12, 0x3559348a, 0x140809b4,
        0x52e008b2, 0xe1ba949e, 0x3b1c46ae, 0xd3531ecb, 0x63103f83, 0x03c5149e,
        0x9f5c4075, 0x91d9e675, 0x64365922, 0x5a18a996, 0x5f58be92, 0x30132b85,
        0x2fd13c6e, 0x95973d42, 0x8bfd616c, 0x23a3cb8e, 0xc4c5e3f2, 0x060ca690,
        0xb2ff5f81, 0x21c81fb3, 0xed0cf8b6, 0xb4e1f7c9, 0x3a48e718, 0xd88a0889,
        0x323a7482, 0xe247fe01, 0xc15e2197, 0x0edc3848, 0x4f17fb42, 0x141525a7,
        0x45a9ca48, 0x1f5f3ef7, 0x85be199f, 0xdd1e0be6, 0xc2ee22f5, 0xb3f1a238,
        0xc46950f2, 0xd653fbda, 0xae793e22, 0xf1ffd00d, 0xed017a52, 0x59ec649c,
        0x69600e12, 0x3c094404, 0x3c2ed4cd, 0xd3ab2fe1, 0xc610c14c, 0xb977759d,
        0x0c648ef3, 0x498a4441, 0x4a806950, 0xdcd56885, 0x76136d42, 0x86705786,
        0xd1552a87, 0xc98b0f20, 0xb7308253, 0x12b59114, 0x48b61c16, 0xd1eb02fb,
        0xdd2d50fb, 0xe6e133e6, 0xd47568ed, 0x4db3cdcf, 0x1993427f, 0x6e8e2fa5,
        0x83e2003c, 0xc555c4fc, 0xdf05e267, 0xa1d5d109, 0x7ed09744, 0x63351d1e,
        0xe8a956d7, 0xe8e6db9d, 0xcbe3407e, 0x2280d908, 0xa6a75f85, 0x61ee3610,
        0x3235b31f, 0xe726190f, 0xc864a66e, 0xe00085a6, 0x6fa0d1df, 0xaf7e0340,
        0x6c6e1ff6, 0x82f25dbd, 0xc27a4d95, 0xb1b69680, 0x2274b7cd, 0xa2c39a79,
        0x9f5a7791, 0xe2a8ab27, 0x464a4d46, 0x69fcf74b, 0x70d8a57c, 0xd1d42878,
        0xe6f03d58, 0x6b093356, 0x2eecf130, 0xe116a4e3, 0x4a8e3d03, 0x8898144b,
        0xeb9143df, 0xd70743b1, 0x6a02b1f8, 0x446e100f, 0xc383c2a3, 0xe252bd8e,
        0xdf727edb, 0xc619ee65, 0xdb3c2859, 0xa8710ee9, 0x05f8846c, 0x9be041c2,
        0x454b15d0, 0xc3c5f6e9, 0x1d374f66, 0xe3e336d3, 0xc2df978d, 0xd888897a,
        0xf2abf27e, 0x3d03caee, 0x6a7d52f6, 0x673dbe8a, 0xa7c223c9, 0xc3191cde,
        0x79403bd9, 0xc75a133f, 0x93307521, 0x1df391d1, 0xba9b6264, 0xf6be6041,
        0x68cd171d, 0x19a0a0cb, 0x1fa12ad9, 0x6a2c206f, 0x7bd8a329, 0xb2f43db1,
        0x83f069bb, 0x8402ee32, 0xdfe861a1, 0xfad6ae6b, 0x2863eb3c, 0xe0a539fe,
        0x9b33a00f, 0xeb4b1ca7, 0x7d656041, 0xdd128e08, 0x00c73229, 0x16b0df22,
        0x19f563d7, 0xae168a3a, 0x40a8309a, 0x3dcf8dbb, 0xc3c94892, 0xdff9e472,
        0x5fb8a071, 0x1536b35b, 0xe4f7574b, 0xb0e872c6, 0xd8fd0a37, 0xe5819ad3,
        0x45db474c, 0xb2838bd2, 0xa3fbde88, 0xa5fb7b54, 0xa8f2df05, 0x3aeb0fbe,
        0xe8a90103, 0xcf0f1ca0, 0x2077e908, 0xfae1136b, 0xf02fa192, 0xbc332f1a,
        0x3235b2e9, 0x13c7bc37, 0x256e2996, 0xad5de7d8, 0xd6b7bf48, 0xe733732c,
        0xd1d4f3b5, 0x0b8a3b3a, 0x61858c14, 0x37850a55, 0x82db0e27, 0x0bc6c075,
        0x93309a63, 0xc0d866a4, 0x63311ef6, 0x936de3e8, 0x0446fae8, 0x10f7fc88,
        0xb029b35b, 0x8f7df43e, 0x9b35d7eb, 0xad9de3a5, 0x6b1d44a2, 0xc555c8fe,
        0xbf309f42, 0xc4eeaf83, 0x3b1c676d, 0x7623dc20, 0xe005a769, 0xe00cf5eb,
        0x805d7620, 0xe08fcdc4, 0x750ff8e5, 0x485af237, 0x1a719c83, 0x51c51886,
        0x4dfc418c, 0xf96b6b71, 0x969dc726, 0x39aef6bc, 0xdf05e0a6, 0x50ba9866,
        0x6a090fe6, 0xca62bb77, 0x39075775, 0x1fbe5343, 0x614f8263, 0x3b664dcf,
        0xbeecbe1f, 0x407bb033, 0x043deedc, 0xfade1d03, 0x1e3bd714, 0xa1d5d36e,
        0xa7d825c0, 0x1ef9798e, 0xbb3273e0, 0x514868e8, 0x76211756, 0xde35fe9f,
        0x19dfa5db, 0x7940250d, 0xc1d5f3bd, 0xbb5c0f44, 0x2280ce56, 0xb734d0b1,
        0xcbe2dc99, 0xbe2512f7, 0xcb99a6cf, 0x66cc9921, 0x9ab8184f, 0xa3fbca25,
        0x20556214, 0x8a928929, 0xe62e49c7, 0x2863eb4f, 0xbe7dca19, 0x356e9c97,
        0x272d5df9, 0x272b5354, 0xe11394a6, 0xe2a8b3be, 0xbbffc15b, 0x2e06ba5d,
        0x7cdc8c36, 0x13d806a0, 0x6335198e, 0x23a3cbbf, 0x31086207, 0x70d843ff,
        0xd1d109f6, 0xc1a12a52, 0xdf05e0fc, 0x07f1ff0d, 0x8a7f9b88, 0xe2f254ad,
        0xbc1ec6de, 0xe26210f0, 0x546e507b, 0x2ffd7220, 0x70d8a576, 0xbeecbd1f,
        0xc864a938, 0x9330a84e, 0x4f17fb03, 0x0446fad8, 0x6fa0d1df, 0x2ffd71ee,
        0x70d8e87d, 0xb2838933
    };

    // Radix 64 encoding table
    private static final char[] bf_crypt_ciphertext = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private static final byte[] index_64 = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
        -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private int[] P;
    private int[] S0;
    private int[] S1;
    private int[] S2;
    private int[] S3;

    private static void encodep(byte[] d, int ofs, int len, StringBuilder sb) {
        int i = ofs;
        int c1, c2;
        while (i < ofs + len) {
            c1 = d[i++] & 0xff;
            sb.append(bf_crypt_ciphertext[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (i >= ofs + len) {
                sb.append(bf_crypt_ciphertext[c1 & 0x3f]);
                break;
            }
            c2 = d[i++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            sb.append(bf_crypt_ciphertext[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (i >= ofs + len) {
                sb.append(bf_crypt_ciphertext[c1 & 0x3f]);
                break;
            }
            c2 = d[i++] & 0xff;
            c1 |= (c2 >> 6) & 0x03;
            sb.append(bf_crypt_ciphertext[c1 & 0x3f]);
            sb.append(bf_crypt_ciphertext[c2 & 0x3f]);
        }
    }

    private static byte char64(char x) {
        if (x < 0 || x >= index_64.length) return -1;
        return index_64[x];
    }

    private static byte[] decodep(String s, int max_ofs) {
        int len = s.length();
        byte[] d = new byte[max_ofs];
        int i = 0, j = 0;
        int c1, c2, c3, c4;
        while (i < len && j < max_ofs) {
            c1 = char64(s.charAt(i++));
            c2 = char64(s.charAt(i++));
            if (c1 == -1 || c2 == -1) return null;
            d[j++] = (byte)((c1 << 2) | ((c2 & 0x30) >> 4));
            if (j >= max_ofs) break;
            c3 = char64(s.charAt(i++));
            if (c3 == -1) return null;
            d[j++] = (byte)(((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            if (j >= max_ofs) break;
            c4 = char64(s.charAt(i++));
            if (c4 == -1) return null;
            d[j++] = (byte)(((c3 & 0x03) << 6) | c4);
        }
        return d;
    }

    private static int streamtodw(byte[] bytes, int[] offset) {
        int dw = 0;
        int off = offset[0];
        
        dw |= (bytes[off] & 0xFF) << 24;
        off = (off + 1 >= bytes.length) ? 0 : off + 1;
        
        dw |= (bytes[off] & 0xFF) << 16;
        off = (off + 1 >= bytes.length) ? 0 : off + 1;
        
        dw |= (bytes[off] & 0xFF) << 8;
        off = (off + 1 >= bytes.length) ? 0 : off + 1;
        
        dw |= (bytes[off] & 0xFF);
        off = (off + 1 >= bytes.length) ? 0 : off + 1;
        
        offset[0] = off;
        return dw;
    }

    private void cipher(int[] lr, int direction) {
        int l = lr[0], r = lr[1];
        if (direction == 1) {
            for (int i = 0; i < BLOWFISH_NUM_ROUNDS; i++) {
                l ^= P[i];
                r ^= F(l);
                int tmp = l; l = r; r = tmp;
            }
            int tmp = l; l = r; r = tmp;
            r ^= P[BLOWFISH_NUM_ROUNDS];
            l ^= P[BLOWFISH_NUM_ROUNDS + 1];
        } else {
            for (int i = BLOWFISH_NUM_ROUNDS + 1; i > 1; i--) {
                l ^= P[i];
                r ^= F(l);
                int tmp = l; l = r; r = tmp;
            }
            int tmp = l; l = r; r = tmp;
            r ^= P[1];
            l ^= P[0];
        }
        lr[0] = l; lr[1] = r;
    }

    private int F(int x) {
        short a = (short)((x >>> 24) & 0xff);
        short b = (short)((x >>> 16) & 0xff);
        short c = (short)((x >>> 8) & 0xff);
        short d = (short)(x & 0xff);
        int y = S0[a] + S1[b];
        y ^= S2[c];
        y += S3[d];
        return y;
    }

    private void initkey() {
        P = P_init.clone();
        S0 = S_init_0.clone();
        S1 = S_init_0.clone();
        S2 = S_init_0.clone();
        S3 = S_init_0.clone();
    }

    private void ekskey(byte[] data, byte[] key) {
        initkey();
        int[] koff = {0}, doff = {0};
        for (int i = 0; i < P_sz; i++) {
            P[i] ^= streamtodw(key, koff);
            if (koff[0] >= key.length) koff[0] = 0;
        }
        int[] lr = {0, 0};
        for (int i = 0; i < P_sz; i += 2) {
            lr[0] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            lr[1] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            cipher(lr, 1);
            P[i] = lr[0];
            P[i+1] = lr[1];
        }
        for (int i = 0; i < 256; i += 2) {
            lr[0] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            lr[1] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            cipher(lr, 1);
            S0[i] = lr[0];
            S0[i+1] = lr[1];
        }
        for (int i = 0; i < 256; i += 2) {
            lr[0] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            lr[1] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            cipher(lr, 1);
            S1[i] = lr[0];
            S1[i+1] = lr[1];
        }
        for (int i = 0; i < 256; i += 2) {
            lr[0] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            lr[1] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            cipher(lr, 1);
            S2[i] = lr[0];
            S2[i+1] = lr[1];
        }
        for (int i = 0; i < 256; i += 2) {
            lr[0] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            lr[1] ^= streamtodw(data, doff);
            if (doff[0] >= data.length) doff[0] = 0;
            cipher(lr, 1);
            S3[i] = lr[0];
            S3[i+1] = lr[1];
        }
    }

    private byte[] crypt_raw(byte[] password, byte[] salt, int log_rounds) {
        int rounds = 1 << log_rounds;
        byte[] key = new byte[password.length + 1];
        System.arraycopy(password, 0, key, 0, password.length);
        ekskey(salt, key);
        for (int i = 0; i < rounds; i++) {
            byte[] pk = new byte[key.length];
            System.arraycopy(key, 0, pk, 0, key.length);
            ekskey(salt, pk);
            ekskey(password, pk);
        }
        byte[] cdata = "OrpheanBeholderScryDoubt".getBytes();
        int[] lr = {0, 0};
        int[] coff = {0};
        for (int i = 0; i < 64; i++) {
            lr[0] = streamtodw(cdata, coff);
            if (coff[0] >= cdata.length) coff[0] = 0;
            lr[1] = streamtodw(cdata, coff);
            if (coff[0] >= cdata.length) coff[0] = 0;
            for (int j = 0; j < 64; j++) {
                cipher(lr, 1);
            }
        }
        byte[] ret = new byte[24];
        for (int i = 0; i < 6; i++) {
            ret[i * 4] = (byte)((lr[0] >> 24) & 0xff);
            ret[i * 4 + 1] = (byte)((lr[0] >> 16) & 0xff);
            ret[i * 4 + 2] = (byte)((lr[0] >> 8) & 0xff);
            ret[i * 4 + 3] = (byte)(lr[0] & 0xff);
        }
        return ret;
    }

    public static String hashpw(String password, String salt) {
        BCrypt B = new BCrypt();
        int real_rounds;
        StringBuilder sb = new StringBuilder();
        int off = 0;
        if (salt.charAt(0) != '$' || salt.charAt(1) != '2' || salt.charAt(2) != 'a' || salt.charAt(3) != '$')
            throw new IllegalArgumentException("Invalid salt version");
        off += 4;
        real_rounds = Integer.parseInt(salt.substring(off, off + 2));
        off += 3;
        byte[] salt_bytes = decodep(salt.substring(off, off + 22), 16);
        byte[] pass_bytes;
        try {
            pass_bytes = password.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            pass_bytes = password.getBytes();
        }
        byte[] hashed = B.crypt_raw(pass_bytes, salt_bytes, real_rounds);
        sb.append("$2a$");
        if (real_rounds < 10) sb.append("0");
        sb.append(real_rounds);
        sb.append("$");
        encodep(salt_bytes, 0, salt_bytes.length, sb);
        encodep(hashed, 0, hashed.length - 1, sb);
        return sb.toString();
    }

    public static String gensalt(int log_rounds) {
        StringBuilder sb = new StringBuilder();
        SecureRandom sr = new SecureRandom();
        byte[] rnd = new byte[16];
        sr.nextBytes(rnd);
        sb.append("$2a$");
        if (log_rounds < 10) sb.append("0");
        sb.append(log_rounds);
        sb.append("$");
        encodep(rnd, 0, rnd.length, sb);
        return sb.toString();
    }

    public static String gensalt() {
        return gensalt(10);
    }

    public static boolean checkpw(String plaintext, String hashed) {
        byte[] pass_bytes;
        try {
            pass_bytes = plaintext.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            pass_bytes = plaintext.getBytes();
        }
        try {
            String new_hashed = hashpw(plaintext, hashed);
            return slowEquals(hashed, new_hashed);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean slowEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }
}
