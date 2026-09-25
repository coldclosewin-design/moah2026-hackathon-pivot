package mobis.vss;

/** 컴파일 전용 스텁. 값은 항상 String. 근거: docs/02_vss_api_contract.md */
public class VSSAppData {

    private String nodePath;
    private String value;

    public VSSAppData() {
    }

    public VSSAppData(String nodePath, String value) {
        this.nodePath = nodePath;
        this.value = value;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "VSSAppData{nodePath=" + nodePath + ", value=" + value + "}";
    }
}
