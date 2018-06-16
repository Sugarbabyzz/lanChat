


public class User {

    /**
     * 个人信息
     * @param name 姓名
     * @param ip ip地址
     */

    private String name;

    private String ip;

    public User(String name, String ip){

        this.name = name;

        this.ip = ip;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getName() {

        return name;
    }

    public void setIp(String ip) {

        this.ip = ip;
    }

    public String getIp() {

        return ip;
    }
}
