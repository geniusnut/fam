package svgandroid;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Stack;

public class IDHandler extends DefaultHandler {
	HashMap<String, String> idXml = new HashMap<String, String>();

	class IdRecording {
		String id;
		int level;
		StringBuilder sb;

		public IdRecording(String id) {
			this.id = id;
			this.level = 0;
			this.sb = new StringBuilder();
		}
	}

	Stack<IdRecording> idRecordingStack = new Stack<IdRecording>();

	/**
	 * @param namespaceURI (unused)
	 * @param qName        (unused)
	 */
	private void appendElementString(StringBuilder sb, String namespaceURI, String localName, String qName, Attributes atts) {
		sb.append("<");
		sb.append(localName);
		for (int i = 0; i < atts.getLength(); i++) {
			sb.append(" ");
			sb.append(atts.getQName(i));
			sb.append("='");
			sb.append(SVGParser.escape(atts.getValue(i)));
			sb.append("'");
		}
		sb.append(">");
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
		String id = atts.getValue("id");
		if (id != null) {
			IdRecording ir = new IdRecording(id);
			idRecordingStack.push(ir);
		}
		if (idRecordingStack.size() > 0) {
			IdRecording ir = idRecordingStack.lastElement();
			ir.level++;
			appendElementString(ir.sb, namespaceURI, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		if (idRecordingStack.size() > 0) {
			IdRecording ir = idRecordingStack.lastElement();
			ir.sb.append("</");
			ir.sb.append(localName);
			ir.sb.append(">");
			ir.level--;
			if (ir.level == 0) {
				String xml = ir.sb.toString();
				//Log.d(TAG, "Added element with id " + ir.id + " and content: " + xml);
				idXml.put(ir.id, xml);
				idRecordingStack.pop();
				if (idRecordingStack.size() > 0) {
					idRecordingStack.lastElement().sb.append(xml);
				}
			}
		}
	}
}
