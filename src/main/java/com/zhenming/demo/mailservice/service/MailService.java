package com.zhenming.demo.mailservice.service;

import com.zhenming.enums.MimeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class MailService {
    //smtp认证用户名
    @Value("${mail.username}")
    private String USERNAME;
    //smtp认证密码
    @Value("${mail.password}")
    private String PASSWORD;
    //发件人使用发邮件的电子信箱服务器
    @Value("${mail.host}")
    private String HOST;
    @Value("${mail.auth}")
    private String MAIL_AUTH;
    @Value("${mail.port}")
    private String PORT;
    @Value("${mail.protocol}")
    private String PROTOCOL;
    @Value("${mail.debug}")
    private String MAIL_DEBUG;
    @Value("${mail.from}")
    private String MAIL_FROM;

    private static Session session;

    private synchronized void sessionInit() {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", HOST);
        if (MAIL_AUTH.equals("true")) {
            props.setProperty("mail.transport.protocol", PROTOCOL);
            props.setProperty("mail.smtp.port", PORT);
            props.setProperty("mail.smtp.auth", MAIL_AUTH);
            props.setProperty("mail.debug", MAIL_DEBUG);
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.ssl.trust", HOST);
            props.setProperty("mail.smtp.ssl.checkserveridentity", "false");
        }
        if (MAIL_AUTH.equals("true")) {
            props.setProperty("mail.transport.protocol", PROTOCOL);
            props.setProperty("mail.smtp.port", PORT);
            props.setProperty("mail.smtp.auth", MAIL_AUTH);
            props.setProperty("mail.debug", MAIL_DEBUG);
            session = Session.getDefaultInstance(props);
        } else {
            session = Session.getInstance(props);
        }
    }

    private Session getSession() {
        if (session == null) {
            sessionInit();
        }
        return session;
    }

    public InternetAddress[] getAddress(String email) throws AddressException {
        InternetAddress[] addresses = new InternetAddress[1];
        addresses[0] = new InternetAddress(email);
        return addresses;
    }

    /**
     * 准备邮件
     */
    private MimeMessage prepareMail(InternetAddress[] to, InternetAddress[] bcc, String subject) throws MessagingException {
        MimeMessage message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress(MAIL_FROM));
        if (to == null) {
            to = new InternetAddress[1];
            to[0] = new InternetAddress(MAIL_FROM);
        }
        message.setRecipients(MimeMessage.RecipientType.TO, to);
        message.setRecipients(MimeMessage.RecipientType.BCC, bcc);
        message.setSubject(subject);
        return message;
    }

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人地址
     * @param bcc     抄送地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendMail(InternetAddress[] to, InternetAddress[] bcc, String subject, String content) throws Exception {
        sendMail(to, bcc, subject, content, null);
    }

    /**
     * 发送带附件纯文本邮件
     *
     * @param to      收件人地址
     * @param bcc     抄送地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param parts   邮件附件数组
     */
    public void sendMail(InternetAddress[] to, InternetAddress[] bcc, String subject,
                         String content, MimeBodyPart[] parts) throws Exception {
        MimeMessage message = prepareMail(to, bcc, subject);
        //判断是否有附件
        if (parts == null) {
            message.setText(content);
        } else {
            message.setContent(addAttachment(content, parts));
        }
        message.saveChanges();
        if (MAIL_AUTH.equals("true")) {
            Transport transport = getSession().getTransport();
            transport.connect(USERNAME, PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } else {
            Transport.send(message);
        }
    }

    /**
     * 发送无附件HTML邮件
     *
     * @param to      收件人地址
     * @param bcc     抄送地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendMail(InternetAddress[] to, InternetAddress[] bcc, String subject, MimeMultipart content) throws Exception {
        sendMail(to, bcc, subject, content, null);
    }

    /**
     * 发送带附件HTML邮件
     *
     * @param to      收件人地址
     * @param bcc     抄送地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param parts   邮件附件数组
     */
    public void sendMail(InternetAddress[] to, InternetAddress[] bcc, String subject,
                         MimeMultipart content, MimeBodyPart[] parts) throws Exception {
        MimeMessage message = prepareMail(to, bcc, subject);
        //判断是否有附件
        if (parts == null) {
            message.setContent(content);
        } else {
            message.setContent(addAttachment(content, parts));
        }
        if (MAIL_AUTH.equals("true")) {
            Transport transport = getSession().getTransport();
            transport.connect(USERNAME, PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } else {
            Transport.send(message);
        }
    }

    /**
     * 为邮件增加附件
     *
     * @param content 邮件文本内容
     * @param parts   邮件附件数组
     */
    private MimeMultipart addAttachment(String content, MimeBodyPart[] parts) throws Exception {
        MimeMultipart part = new MimeMultipart();
        for (MimeBodyPart bodyPart : parts) {
            part.addBodyPart(bodyPart);
        }
        MimeBodyPart contentPart = new MimeBodyPart();
        contentPart.setText(content);
        part.addBodyPart(contentPart);
        return part;
    }

    /**
     * 为邮件增加附件
     *
     * @param content 邮件HTML内容
     * @param parts   邮件附件数组
     */
    private MimeMultipart addAttachment(MimeMultipart content, MimeBodyPart[] parts) throws Exception {
        MimeMultipart part = new MimeMultipart();
        for (MimeBodyPart bodyPart : parts) {
            part.addBodyPart(bodyPart);
        }
        MimeBodyPart contentPart = new MimeBodyPart();
        contentPart.setContent(content);
        part.addBodyPart(contentPart);
        return part;
    }

    /**
     * 将附件文件转为MimeBodyPart
     *
     * @param is       附件流
     * @param fileName 附件名称
     * @param type     附件类型
     */
    public MimeBodyPart generateInputStream(InputStream is, String fileName, MimeType type) throws Exception {
        MimeBodyPart mbp = new MimeBodyPart();
        DataHandler handler = new DataHandler(new ByteArrayDataSource(is, type.getValue()));
        mbp.setDataHandler(handler);
        mbp.setFileName(MimeUtility.encodeText(fileName, "UTF-8", "B"));
        return mbp;
    }

    public MimeBodyPart[] generateAttachmentArray(MimeBodyPart... parts) {
        return parts;
    }

    /**
     * 将图片文件装成MimeBodyPart
     *
     * @param is   包装的图片流
     * @param type MIME Type
     * @param cid  生成的cid
     */
    public MimeBodyPart loadImage(InputStream is, MimeType type, String cid) throws MessagingException, IOException {
        MimeBodyPart image = new MimeBodyPart();
        DataSource dataSource = new ByteArrayDataSource(is, type.getValue());
        DataHandler dh = new DataHandler(dataSource);
        image.setDataHandler(dh);
        image.setContentID(cid);
        return image;
    }
}
