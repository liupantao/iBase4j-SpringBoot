package top.ibase4j.core.support.email;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import top.ibase4j.core.support.context.Resources;
import top.ibase4j.core.util.PropertiesUtil;

/**
 * 邮件引擎
 *
 * @author ShenHuaJie
 * @version $Id: MailEntrance.java, v 0.1 2014年12月4日 下午8:34:48 ShenHuaJie Exp $
 */
public final class EmailSender {
    private final Logger logger = LogManager.getLogger();

    private MimeMessage mimeMsg; // MIME邮件对象
    private Session session; // 邮件会话对象
    private Properties props; // 系统属性
    private final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    private String username = ""; // smtp认证用户名和密码
    private String password = "";
    private String userkey = "";
    private boolean isSSL;

    private Multipart mp; // Multipart对象,邮件内容,标题,附件等内容均添加到其中后再生成MimeMessage对象

    /**
     * @param smtp
     */
    public EmailSender(String host, String port, boolean isSSL) {
        try {
            this.isSSL = isSSL;
            if (host == null || host.trim().equals("")) {
                host = PropertiesUtil.getString("email.smtp.host");
            }
            logger.info(Resources.getMessage("EMAIL.SET_HOST"), host);
            if (props == null) {
                props = System.getProperties(); // 获得系统属性对象
            }
            props.put("mail.smtp.host", host); // 设置SMTP主机
            if (port == null || port.trim().equals("")) {
                port = PropertiesUtil.getString("email.smtp.port");
            }
            if (port == null || port.trim().equals("")) {
                props.put("mail.smtp.port", port);
            } else {
                props.put("mail.smtp.port", "25");
            }
            if ("smtp.gmail.com".equals(host) || isSSL) {
                props.put("mail.smtp.port", "465");
                props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
                props.setProperty("mail.smtp.socketFactory.fallback", "false");
                props.setProperty("mail.smtp.socketFactory.port", "465");
            }
            if (!createMimeMessage()) {
                throw new RuntimeException("创建MIME邮件对象和会话失败");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param name String
     * @param pass String
     * @return
     */
    public boolean setNamePass(String name, String pass, String key) {
        if (name == null || name.trim().equals("")) {
            name = PropertiesUtil.getString("email.user.name");
        }
        if (pass == null || pass.trim().equals("")) {
            pass = PropertiesUtil.getString("email.user.password");
        }
        username = name;
        password = pass;
        userkey = key;
        setNeedAuth();
        return createMimeMessage();
    }

    /**
     *
     */
    private void setNeedAuth() {
        if (props == null) {
            props = System.getProperties();
        }
        if (userkey == null || userkey.trim().equals("")) {
            userkey = PropertiesUtil.getString("email.authorisation.code");
        }
        if (userkey == null || userkey.trim().equals("")) {
            props.setProperty("mail.smtp.auth", "false");
            logger.info(Resources.getMessage("EMAIL.SET_AUTH"), "false");
        } else {
            props.setProperty("mail.smtp.auth", "true");
            logger.info(Resources.getMessage("EMAIL.SET_AUTH"), "true");
        }
    }

    /**
     * 创建MIME邮件对象
     *
     * @return boolean
     */
    private boolean createMimeMessage() {
        if (session == null) {
            try {
                // 获得邮件会话对象
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (userkey == null || "".equals(userkey.trim())) {
                            return null;
                        }
                        if (isSSL) {
                            return new PasswordAuthentication(username, password);
                        }
                        return new PasswordAuthentication(username, userkey);
                    }
                });
            } catch (Exception e) {
                logger.error(Resources.getMessage("EMAIL.ERROR_TALK"), e.getLocalizedMessage());
                return false;
            }
        }
        if (mimeMsg == null) {
            try {
                mimeMsg = new MimeMessage(session); // 创建MIME邮件对象
                mp = new MimeMultipart();
                return true;
            } catch (Exception e) {
                logger.error(Resources.getMessage("EMAIL.ERROR_MIME"), e.getLocalizedMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * 设置主题
     *
     * @param mailSubject String
     * @return boolean
     */
    public boolean setSubject(String mailSubject) {
        logger.info(Resources.getMessage("EMAIL.SET_SUBJECT"), mailSubject);
        try {
            mimeMsg.setSubject(mailSubject, "UTF-8");
            return true;
        } catch (Exception e) {
            logger.error(Resources.getMessage("EMAIL.ERROR_SUBJECT"), e);
            return false;
        }
    }

    /**
     * 设置内容
     *
     * @param mailBody String
     */
    public boolean setBody(String mailBody) {
        try {
            BodyPart bp = new MimeBodyPart();
            bp.setContent("" + mailBody, "text/html;charset=UTF-8");
            mp.addBodyPart(bp);
            return true;
        } catch (Exception e) {
            logger.error(Resources.getMessage("EMAIL.ERROR_BODY"), e);
            return false;
        }
    }

    /**
     * 设置附件
     *
     * @param filename
     * @return
     */
    public boolean addFileAffix(String filename) {
        logger.info(Resources.getMessage("EMAIL.ADD_ATTEND"), filename);
        try {
            BodyPart bp = new MimeBodyPart();
            FileDataSource fileds = new FileDataSource(filename);
            bp.setDataHandler(new DataHandler(fileds));
            bp.setFileName(MimeUtility.encodeText(fileds.getName()));
            mp.addBodyPart(bp);
            return true;
        } catch (Exception e) {
            logger.error(filename, e);
            return false;
        }
    }

    /**
     * 设置发信人
     *
     * @param from
     * @return
     */
    public boolean setFrom(String from) {
        if (from == null || from.trim().equals("")) {
            from = PropertiesUtil.getString("email.send.from");
        }
        try {
            String[] f = from.split(",");
            if (f.length > 1) {
                from = MimeUtility.encodeText(f[0]) + "<" + f[1] + ">";
            }
            mimeMsg.setFrom(new InternetAddress(from)); // 设置发信人
            return true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * 设置收信人
     *
     * @param to
     * @return
     */
    public boolean setTo(String to) {
        if (to == null) {
            return false;
        }
        logger.info(Resources.getMessage("EMAIL.SET_TO"), to);
        try {
            mimeMsg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            return true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * 设置抄送人
     * @param copyto
     * @return
     */
    public boolean setCopyTo(String copyto) {
        if (copyto == null) {
            return false;
        }
        logger.info(Resources.getMessage("EMAIL.SET_COPYTO"), copyto);
        try {
            mimeMsg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(copyto));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 发送邮件
     */
    public boolean sendout() {
        try {
            mimeMsg.setContent(mp);
            // 设置发送日期
            mimeMsg.setSentDate(new Date());
            mimeMsg.saveChanges();

            logger.info(Resources.getMessage("EMAIL.SENDING"));
            // 发送
            Transport.send(mimeMsg);
            logger.info(Resources.getMessage("EMAIL.SEND_SUCC"));
            return true;
        } catch (Exception e) {
            logger.error(Resources.getMessage("EMAIL.SEND_ERR"), e);
            return false;
        }
    }
}
