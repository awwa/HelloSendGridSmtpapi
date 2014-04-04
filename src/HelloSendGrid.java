import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;


public class HelloSendGrid {
    @SuppressWarnings("unused")
    private static final String TAG = HelloSendGrid.class.getSimpleName();
    private final HelloSendGrid self = this;

    private static final String SMTP_HOST_NAME = "smtp.sendgrid.net";
    private static final String SMTP_AUTH_USER = "SENDGRID_USERNAME";   // TODO change
    private static final String SMTP_AUTH_PWD = "SENDGRID_PASSWORD";    // TODO change
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        HelloSendGrid instance = new HelloSendGrid();
        instance.testSMTPAPI();
    }
    
    public void testSMTPAPI() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", SMTP_HOST_NAME);
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.auth", "true");
        
        Authenticator auth = new SMTPAuthenticator();
        Session mailSession = Session.getDefaultInstance(props, auth);
        mailSession.setDebug(true);
        Transport transport = mailSession.getTransport();
        MimeMessage message = new MimeMessage(mailSession);
        
        // X-SMTPAPIヘッダ値
        String smtpApiHeaderValue =  
                "{" +
                        "\"category\": [\"category1\", \"category2\" ]," +
                        "\"to\": [\"awwa500@gmail.com\", \"awwa502@gmail.com\"]," +     // TODO change to
                        "\"sub\": { " +
                                "\"-body-\": [ \"-bodyFemale-\", \"-bodyMale-\" ], " +
                                "\"-favorite-\": [ \"バナナ\", \"カツオ\" ], " +
                                "\"-nickname-\": [ \"ミキ\", \"サブロー\" ] }, " +
                        "\"section\": { " +
                                "\"-bodyFemale-\": \"-nickname-さん！女性向けの商品のご紹介です。\", " +
                                "\"-bodyMale\": \"-nickname-さん！男性向けの商品のご紹介です。\" }" +
                  "}";
        // X-SMTPAPIヘッダ値をRFC1522に従ったエンコード（ISO-2022-JP＋Base64でエンコード）
        String encSmtpApiHeaderValue = encodeRfc1522(smtpApiHeaderValue);
        // X-SMTPAPIヘッダ追加
        message.setHeader("X-SMTPAPI", encSmtpApiHeaderValue);

        Multipart multipart = new MimeMultipart("alternative");
        
        BodyPart part1 = new MimeBodyPart();
        part1.setText("ようこそ！\n -body- ");
        
        BodyPart part2 = new MimeBodyPart();
        part2.setContent("<b>ようこそ！\n -body- </b>", "text/html;charset=iso-2022-jp");
        
        multipart.addBodyPart(part1);
        multipart.addBodyPart(part2);
        
        message.setContent(multipart);
        message.setFrom("awwa500@gmail.com");                   // TODO change from
        message.setSubject("ようこそ-favorite-好きの-nickname-さん！");
        message.addRecipient(Message.RecipientType.BCC,
                new InternetAddress("awwa500@gmail.com"));      // TODO change bcc
        
        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.BCC));
        transport.close();
    }
    
    /**
     * RFC1522に従ったエンコードを行う。
     * @param header
     * @return
     */
    private String encodeRfc1522(String header) {
        byte[] enc = unicode2iso2022jp(header);
        String base64 = Base64.encodeBase64String(enc);
        String ret = String.format("=?ISO-2022-JP?B?%s?=", base64);
        return ret;
    }
    
    /**
     * UnicodeをISO-2022-JPに変換する
     * @param unicode
     * @return
     */
    private byte[] unicode2iso2022jp(String unicode) {
        CharsetEncoder jisEncoder = Charset.forName("ISO-2022-JP").newEncoder();
        ByteBuffer encoded = null;
        byte[] capacity = null;
        List<Byte> temp = new ArrayList<Byte>();
        byte[] result = null;

        //(Windows-31J -> Unicode) -> (ISO-2022-JP -> Unicode)
        unicode = unicode.replaceAll("\uff5e", "\u301c"); //～
        unicode = unicode.replaceAll("\uff0d", "\u2212"); //－

        try {
            encoded = jisEncoder.encode(CharBuffer.wrap(unicode.toCharArray()));
            capacity = encoded.array();
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < encoded.limit(); i++) {
            temp.add(capacity[i]);
        }

        result = new byte[temp.size()];
        
        for (int i = 0; i < result.length; i++) {
            result[i] = temp.get(i);
        }

        return result;
    }
    
    private class SMTPAuthenticator extends javax.mail.Authenticator { 
        public PasswordAuthentication getPasswordAuthentication() {
            String username = SMTP_AUTH_USER;
            String password = SMTP_AUTH_PWD;
            return new PasswordAuthentication(username, password);
        }
    }
}
