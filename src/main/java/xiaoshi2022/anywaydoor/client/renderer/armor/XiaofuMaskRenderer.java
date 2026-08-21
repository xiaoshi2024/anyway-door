package xiaoshi2022.anywaydoor.client.renderer.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;
import xiaoshi2022.anywaydoor.item.XiaofuMaskItem;

public class XiaofuMaskRenderer extends GeoArmorRenderer<XiaofuMaskItem> {

    public XiaofuMaskRenderer() {
        super(new XiaofuMaskModel());
    }
}