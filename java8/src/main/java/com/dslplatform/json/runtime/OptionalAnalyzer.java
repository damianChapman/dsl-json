package com.dslplatform.json.runtime;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;

import java.lang.reflect.*;
import java.util.Optional;

public abstract class OptionalAnalyzer {

	private static final JsonWriter.WriteObject tmpWriter = (writer, value) -> {
		throw new IllegalStateException("Invalid configuration for writer. Temporary writer called");
	};
	private static final JsonReader.ReadObject tmpReader = reader -> {
		throw new IllegalStateException("Invalid configuration for reader. Temporary reader called");
	};

	public static final DslJson.ConverterFactory<OptionalDecoder> READER = (manifest, dslJson) -> {
		if (manifest instanceof ParameterizedType) {
			final ParameterizedType pt = (ParameterizedType) manifest;
			if (pt.getActualTypeArguments().length == 1 && pt.getRawType() instanceof Class<?>) {
				return analyzeDecoding(manifest, pt.getActualTypeArguments()[0], (Class<?>) pt.getRawType(), dslJson);
			}
		}
		return null;
	};

	public static final DslJson.ConverterFactory<OptionalEncoder> WRITER = (manifest, dslJson) -> {
		if (manifest instanceof ParameterizedType) {
			final ParameterizedType pt = (ParameterizedType) manifest;
			if (pt.getActualTypeArguments().length == 1 && pt.getRawType() instanceof Class<?>) {
				return analyzeEncoding(manifest, pt.getActualTypeArguments()[0], (Class<?>) pt.getRawType(), dslJson);
			}
		}
		return null;
	};

	private static OptionalDecoder analyzeDecoding(final Type manifest, final Type content, final Class<?> raw, final DslJson json) {
		if (raw != Optional.class) {
			return null;
		} else if (content == Optional.class) {
			final OptionalDecoder nested = analyzeDecoding(content, Object.class, Optional.class, json);
			final OptionalDecoder outer = new OptionalDecoder<>(nested);
			json.registerReader(manifest, outer);
			return outer;
		}
		final JsonReader.ReadObject oldReader = json.registerReader(manifest, tmpReader);
		final JsonReader.ReadObject<?> reader = json.tryFindReader(content);
		if (reader == null) {
			json.registerReader(manifest, oldReader);
			return null;
		}
		final OptionalDecoder decoder = new OptionalDecoder<>(reader);
		json.registerReader(manifest, decoder);
		return decoder;
	}

	private static OptionalEncoder analyzeEncoding(final Type manifest, final Type content, final Class<?> raw, final DslJson json) {
		if (raw != Optional.class) {
			return null;
		} else if (content == Optional.class) {
			final OptionalEncoder nested = analyzeEncoding(content, Object.class, Optional.class, json);
			json.registerWriter(manifest, nested);
			return nested;
		}
		final JsonWriter.WriteObject oldWriter = json.registerWriter(manifest, tmpWriter);
		final JsonWriter.WriteObject<?> writer = json.tryFindWriter(content);
		if (Object.class != content && writer == null) {
			json.registerWriter(manifest, oldWriter);
			return null;
		}
		final OptionalEncoder encoder = new OptionalEncoder<>(json, Object.class == content ? null : writer);
		json.registerWriter(manifest, encoder);
		return encoder;
	}
}