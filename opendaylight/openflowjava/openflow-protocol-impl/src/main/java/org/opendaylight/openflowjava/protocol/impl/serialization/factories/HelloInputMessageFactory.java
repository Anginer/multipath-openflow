/*
 * Copyright (c) 2013 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowjava.protocol.impl.serialization.factories;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.openflowjava.protocol.impl.serialization.OFSerializer;
import org.opendaylight.openflowjava.protocol.impl.util.ByteBufUtils;
import org.opendaylight.openflowjava.protocol.impl.util.EncodeConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.HelloElementType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.Elements;

/**
 * Translates Hello messages
 * @author michal.polkorab
 * @author timotej.kubas
 */
public class HelloInputMessageFactory implements OFSerializer<HelloInput>{

    /** Code type of Hello message */
    private static final byte MESSAGE_TYPE = 0;
    private static int MESSAGE_LENGTH = 8;
    /** Size of hello element header (in bytes) */
    public static final byte HELLO_ELEMENT_HEADER_SIZE = 4;
    private static HelloInputMessageFactory instance;

    private HelloInputMessageFactory() {
        // do nothing, just singleton
    }
    
    /**
     * @return singleton factory
     */
    public static synchronized HelloInputMessageFactory getInstance() {
        if (instance == null) {
            instance = new HelloInputMessageFactory();
        }
        return instance;
    }

    @Override
    public void messageToBuffer(short version, ByteBuf out, HelloInput message) {
        int startWriterIndex = out.writerIndex();
        ByteBufUtils.writeOFHeader(instance, message, out);
        encodeElementsList(message, out);
        int endWriterIndex = out.writerIndex();
        int writtenBytesDiff = computeLength(message) - (endWriterIndex - startWriterIndex);
        ByteBufUtils.padBuffer(writtenBytesDiff, out);
    }

    @Override
    public int computeLength(HelloInput message) {
        int length = MESSAGE_LENGTH;
        List<Elements> elements = message.getElements();
        if (elements != null) {
            for (Elements element : elements) {
                if (HelloElementType.VERSIONBITMAP.equals(element.getType())) {
                    int bitmapLength = computeVersionBitmapLength(element);
                    int paddingRemainder = bitmapLength % EncodeConstants.PADDING;
                    if (paddingRemainder != 0) {
                        bitmapLength += EncodeConstants.PADDING - paddingRemainder;
                    }
                    length += bitmapLength;
                }
            }
        }
        return length;
    }

    @Override
    public byte getMessageType() {
        return MESSAGE_TYPE;
    }
    
    private static void encodeElementsList(HelloInput message, ByteBuf output) {
        int[] versionBitmap;
        if (message.getElements() != null) {
            for (Elements currElement : message.getElements()) {
                output.writeShort(currElement.getType().getIntValue());
                if (currElement.getType().equals(HelloElementType.VERSIONBITMAP)) {
                    short bitmapLength = computeVersionBitmapLength(currElement);
                    output.writeShort(bitmapLength);
                    versionBitmap = ByteBufUtils.fillBitMaskFromList(currElement.getVersionBitmap());
                    for (int i = 0; i < versionBitmap.length; i++) {
                        output.writeInt(versionBitmap[i]);
                    }
                    int padding = bitmapLength - versionBitmap.length * 4 - HELLO_ELEMENT_HEADER_SIZE;
                    ByteBufUtils.padBuffer(padding , output);
                }
            } 
        }
    }
    
    private static short computeVersionBitmapLength(Elements element) {
        short elementlength = HELLO_ELEMENT_HEADER_SIZE;
        if (!element.getVersionBitmap().isEmpty()) {
            elementlength += ((element.getVersionBitmap().size() - 1) / Integer.SIZE + 1) * (EncodeConstants.SIZE_OF_INT_IN_BYTES);
        }
        return elementlength;
    }
}
